package com.geosegbar.infra.psb.services;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public PSBFileEntity uploadFile(Long folderId, MultipartFile file, Long uploadedById) {
        validateEditPermission();

        PSBFolderEntity folder = psbFolderRepository.findById(folderId)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));

        UserEntity uploader = userRepository.findById(uploadedById)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        try {
            String s3Directory = sanitizeFolderPath(folder.getServerPath());
            String downloadUrl = fileStorageService.storeFile(file, s3Directory);
            String filename = extractFilenameFromUrl(downloadUrl);
            String s3Key = s3Directory + "/" + filename;

            PSBFileEntity psbFile = new PSBFileEntity();
            psbFile.setFilename(filename);
            psbFile.setOriginalFilename(file.getOriginalFilename());
            psbFile.setContentType(file.getContentType());
            psbFile.setSize(file.getSize());
            psbFile.setPsbFolder(folder);
            psbFile.setUploadedBy(uploader);
            psbFile.setFilePath(s3Key);
            psbFile.setDownloadUrl(downloadUrl);

            log.info("Arquivo PSB enviado para S3: {}", s3Key);

            return psbFileRepository.save(psbFile);

        } catch (Exception ex) {
            log.error("Erro no upload PSB: {}", ex.getMessage());
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
