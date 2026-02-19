package com.geosegbar.infra.psb.services;

import java.time.Instant;
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
import com.geosegbar.infra.psb.dtos.PresignedUploadCompleteRequest;
import com.geosegbar.infra.psb.dtos.PresignedUploadInitRequest;
import com.geosegbar.infra.psb.dtos.PresignedUploadInitResponse;
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
    private final PresignedUploadService presignedUploadService;
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

    // =========================================================================
    // PRESIGNED URL UPLOAD — Laravel envia direto ao S3 (SEMPRE multipart)
    // =========================================================================
    /**
     * FASE 1: Inicializa upload via URL pré-assinada.
     *
     * Valida permissões, pasta e usuário, gera S3 key e retorna URLs
     * pré-assinadas para multipart upload. Laravel usa as URLs para enviar o
     * arquivo diretamente ao S3 (sem passar pelo Spring).
     *
     * TODOS os arquivos usam multipart — sem exceção. Part sizing dinâmico
     * conforme tamanho do arquivo.
     */
    public PresignedUploadInitResponse initPresignedUpload(Long folderId, PresignedUploadInitRequest request) {
        validateEditPermission();

        log.info("[PSB PRESIGNED] Iniciando: arquivo='{}', tamanho={}MB, pasta={}, usuario={}",
                request.getFilename(),
                String.format("%.1f", request.getFileSize() / (1024.0 * 1024.0)),
                folderId, request.getUploadedById());

        // Valida pasta e usuário (transação curta, read-only)
        TransactionTemplate readTx = new TransactionTemplate(transactionManager);
        readTx.setReadOnly(true);

        String s3Directory = readTx.execute(status -> {
            String serverPath = psbFolderRepository.findServerPathById(folderId)
                    .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));

            if (!userRepository.existsById(request.getUploadedById())) {
                throw new NotFoundException("Usuário não encontrado");
            }

            return sanitizeFolderPath(serverPath);
        });

        // Gera nome único para o arquivo no S3
        String originalFilename = request.getFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String safeFileName = timestamp + "_" + (originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "file" + fileExtension);
        String s3Key = s3Directory + "/" + safeFileName;

        // Delega ao PresignedUploadService para gerar URLs
        return presignedUploadService.initUpload(
                s3Key, request.getFileSize(), request.getContentType(),
                folderId, request.getUploadedById()
        );
    }

    /**
     * FASE 2: Confirma que o upload direto ao S3 foi concluído.
     *
     * Verifica o arquivo no S3 (ou completa multipart), depois cria a entidade
     * PSBFileEntity no banco em uma transação curta.
     *
     * Garante consistência: se falhar ao salvar no banco, o arquivo fica no S3
     * como órfão (cleanup pode ser feito depois) — melhor do que perder dados.
     */
    public PSBFileEntity completePresignedUpload(Long folderId, PresignedUploadCompleteRequest request) {
        validateEditPermission();

        // Confirma upload no S3 (verifica existência ou completa multipart)
        PresignedUploadService.CompleteResult result
                = presignedUploadService.completeUpload(request.getUploadId(), request.getCompletedParts());

        // Extrai filename da s3Key
        String filename = extractFilenameFromUrl(result.s3Key());

        log.info("[PSB PRESIGNED] Confirmado, salvando no banco: s3Key='{}', tamanho={}MB, pasta={}",
                result.s3Key(),
                String.format("%.1f", result.fileSize() / (1024.0 * 1024.0)),
                folderId);

        // Salva no banco (transação curta)
        TransactionTemplate writeTx = new TransactionTemplate(transactionManager);
        return writeTx.execute(status -> {
            PSBFolderEntity folderRef = psbFolderRepository.getReferenceById(folderId);
            UserEntity uploaderRef = userRepository.getReferenceById(result.uploadedById());

            PSBFileEntity psbFile = new PSBFileEntity();
            psbFile.setFilename(filename);
            psbFile.setOriginalFilename(extractOriginalName(filename));
            psbFile.setContentType(result.contentType());
            psbFile.setSize(result.fileSize());
            psbFile.setPsbFolder(folderRef);
            psbFile.setUploadedBy(uploaderRef);
            psbFile.setFilePath(result.s3Key());
            psbFile.setDownloadUrl(result.downloadUrl());

            PSBFileEntity saved = psbFileRepository.save(psbFile);
            log.info("[PSB PRESIGNED] Arquivo salvo no banco: id={}, filename='{}'", saved.getId(), filename);
            return saved;
        });
    }

    /**
     * Cancela um upload pré-assinado em andamento. Se multipart, aborta o
     * upload no S3 para liberar fragmentos.
     */
    public void abortPresignedUpload(String uploadId) {
        validateEditPermission();
        presignedUploadService.abortUpload(uploadId);
        log.info("[PSB PRESIGNED] Upload cancelado: uploadId={}", uploadId);
    }

    /**
     * Extrai o nome original removendo o timestamp prefix. Ex:
     * "1708300000_relatorio_mensal.pdf" → "relatorio_mensal.pdf"
     */
    private String extractOriginalName(String filename) {
        if (filename == null) {
            return null;
        }
        int underscoreIdx = filename.indexOf('_');
        if (underscoreIdx > 0 && underscoreIdx < filename.length() - 1) {
            // Verifica se o prefixo parece um timestamp (só dígitos)
            String prefix = filename.substring(0, underscoreIdx);
            if (prefix.matches("\\d+")) {
                return filename.substring(underscoreIdx + 1);
            }
        }
        return filename;
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId) {
        validateViewPermission();
        try {
            PSBFileEntity file = psbFileRepository.findById(fileId)
                    .orElseThrow(() -> new NotFoundException("Arquivo PSB não encontrado"));

            Resource resource = new UrlResource(java.net.URI.create(file.getDownloadUrl()));

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Não foi possível ler o arquivo do S3");
            }

        } catch (java.net.MalformedURLException ex) {
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
