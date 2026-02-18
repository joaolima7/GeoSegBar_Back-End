package com.geosegbar.infra.psb.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.psb.persistence.PSBFileRepository;
import com.geosegbar.infra.psb.persistence.PSBFolderRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PSBFileService {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final PSBFileRepository psbFileRepository;
    private final PSBFolderRepository psbFolderRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PlatformTransactionManager transactionManager;

    @Transactional(readOnly = true)
    public List<PSBFileEntity> findByFolderId(Long folderId) {
        validateViewPermission();
        return psbFileRepository.findByPsbFolderIdOrderByUploadedAtDesc(folderId);
    }

    @Transactional(readOnly = true)
    public PSBFileEntity findById(Long id) {
        validateViewPermission();
        return psbFileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Arquivo PSB não encontrado"));
    }

    /**
     * Upload de arquivo PSB.
     *
     * IMPORTANTE: Este método NÃO é @Transactional porque o upload S3 pode
     * levar dezenas de segundos. Manter uma transação/conexão DB aberta durante
     * todo esse tempo causa: 1) transaction timeout
     * (spring.transaction.default-timeout=30) 2) Connection leak detection
     * (hikari.leak-detection-threshold=30000) 3) Desperdício de conexões do
     * pool
     *
     * Solução: 3 fases com transações curtas e independentes.
     */
    public PSBFileEntity uploadFile(Long folderId, MultipartFile file, Long uploadedById) {
        validateEditPermission();

        long fileSizeBytes = file.getSize();
        double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
        log.info("[PSB UPLOAD] Iniciando upload: arquivo='{}', tamanho={} bytes ({} MB), pasta={}, usuario={}",
                file.getOriginalFilename(), fileSizeBytes, String.format("%.2f", fileSizeMB), folderId, uploadedById);

        // ============================================================
        // FASE 1: Carregar dados e validar (transação curta, read-only)
        // ============================================================
        TransactionTemplate readTx = new TransactionTemplate(transactionManager);
        readTx.setReadOnly(true);

        String s3Directory = readTx.execute(status -> {
            // Query enxuta: busca APENAS serverPath (1 SELECT simples)
            // Evita o EntityGraph do findById que carrega dam → checklists → documentation_dam (N+1)
            String serverPath = psbFolderRepository.findServerPathById(folderId)
                    .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));

            if (!userRepository.existsById(uploadedById)) {
                throw new NotFoundException("Usuário não encontrado");
            }

            return sanitizeFolderPath(serverPath);
        });

        // ============================================================
        // FASE 2: Upload para S3 (SEM transação, SEM conexão DB)
        // Pode levar segundos ou minutos — não segura recurso do pool
        // ============================================================
        try {
            long s3Start = System.currentTimeMillis();
            String downloadUrl = fileStorageService.storeFile(file, s3Directory);
            long s3Elapsed = System.currentTimeMillis() - s3Start;

            String filename = extractFilenameFromUrl(downloadUrl);
            String s3Key = s3Directory + "/" + filename;

            log.info("[PSB UPLOAD] Arquivo enviado ao S3 com sucesso: s3Key='{}', tempo_s3={}ms ({} MB/s)",
                    s3Key, s3Elapsed,
                    s3Elapsed > 0 ? String.format("%.2f", fileSizeMB / (s3Elapsed / 1000.0)) : "N/A");

            // ============================================================
            // FASE 3: Salvar entidade no banco (transação curta, write)
            // Usa getReferenceById para evitar SELECT extra — só precisa do ID
            // para setar a FK no INSERT
            // ============================================================
            TransactionTemplate writeTx = new TransactionTemplate(transactionManager);
            return writeTx.execute(status -> {
                PSBFolderEntity folderRef = psbFolderRepository.getReferenceById(folderId);
                UserEntity uploaderRef = userRepository.getReferenceById(uploadedById);

                PSBFileEntity psbFile = new PSBFileEntity();
                psbFile.setFilename(filename);
                psbFile.setOriginalFilename(file.getOriginalFilename());
                psbFile.setContentType(file.getContentType());
                psbFile.setSize(file.getSize());
                psbFile.setPsbFolder(folderRef);
                psbFile.setUploadedBy(uploaderRef);
                psbFile.setFilePath(s3Key);
                psbFile.setDownloadUrl(downloadUrl);

                return psbFileRepository.save(psbFile);
            });

        } catch (Exception ex) {
            log.error("[PSB UPLOAD] Erro no upload PSB: arquivo='{}', tamanho={} MB, pasta={}: {}",
                    file.getOriginalFilename(), String.format("%.2f", fileSizeMB), folderId, ex.getMessage());
            throw new FileStorageException("Não foi possível armazenar o arquivo no S3.", ex);
        }
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId) {
        validateViewPermission();
        try {
            PSBFileEntity file = psbFileRepository.findById(fileId)
                    .orElseThrow(() -> new NotFoundException("Arquivo PSB não encontrado"));

            Resource resource = new UrlResource(URI.create(file.getDownloadUrl()));

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Não foi possível ler o arquivo do S3");
            }

        } catch (MalformedURLException ex) {
            throw new FileStorageException("URL do arquivo inválida: " + ex.getMessage(), ex);
        }
    }

    @Transactional
    public void deleteFile(Long fileId) {
        validateEditPermission();

        PSBFileEntity file = psbFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Arquivo PSB não encontrado"));

        fileStorageService.deleteFile(file.getDownloadUrl());

        psbFileRepository.delete(file);

        log.info("Arquivo PSB excluído: {} (ID: {})", file.getFilename(), fileId);
    }

    private String sanitizeFolderPath(String serverPath) {
        if (serverPath == null) {
            return "uploads";
        }

        String cleanPath = serverPath;
        if (cleanPath.contains("/storage/app/public/")) {
            cleanPath = cleanPath.substring(cleanPath.indexOf("/storage/app/public/") + 20);
        } else if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        if (cleanPath.endsWith("/")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        }

        return cleanPath;
    }

    private String extractFilenameFromUrl(String url) {
        if (url == null) {
            return null;
        }
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
            return url.substring(lastSlashIndex + 1);
        }
        return url;
    }

    /**
     * Valida se o usuário atual tem permissão de visualização de PSB.
     * Administradores têm permissão automática.
     */
    private void validateViewPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (user.getDocumentationPermission() == null || !Boolean.TRUE.equals(user.getDocumentationPermission().getViewPSB())) {
                throw new NotFoundException("Usuário não tem permissão para visualizar arquivos PSB!");
            }
        }
    }

    /**
     * Valida se o usuário atual tem permissão de edição de PSB. Administradores
     * têm permissão automática.
     */
    private void validateEditPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (user.getDocumentationPermission() == null || !Boolean.TRUE.equals(user.getDocumentationPermission().getEditPSB())) {
                throw new NotFoundException("Usuário não tem permissão para editar arquivos PSB!");
            }
        }
    }
}
