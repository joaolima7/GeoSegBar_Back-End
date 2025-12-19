package com.geosegbar.infra.psb.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

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
import com.geosegbar.infra.psb.persistence.PSBFileRepository;
import com.geosegbar.infra.psb.persistence.PSBFolderRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PSBFileService {

    @Value("${file.base-url}")
    private String baseUrl;

    private final PSBFileRepository psbFileRepository;
    private final PSBFolderRepository psbFolderRepository;
    private final UserRepository userRepository;

    public List<PSBFileEntity> findByFolderId(Long folderId) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getViewPSB()) {
                throw new NotFoundException("Usuário não tem permissão para acessar as pastas PSB");
            }
        }
        return psbFileRepository.findByPsbFolderIdOrderByUploadedAtDesc(folderId);
    }

    public PSBFileEntity findById(Long id) {
        return psbFileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Arquivo PSB não encontrado"));
    }

    @Transactional
    public PSBFileEntity uploadFile(Long folderId, MultipartFile file, Long uploadedById) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getEditPSB()) {
                throw new NotFoundException("Usuário não tem permissão para enviar arquivos PSB");
            }
        }
        try {
            PSBFolderEntity folder = psbFolderRepository.findById(folderId)
                    .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));

            UserEntity uploader = userRepository.findById(uploadedById)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = UUID.randomUUID().toString() + fileExtension;

            // Caminho completo onde o arquivo será salvo
            Path folderPath = Paths.get(folder.getServerPath());
            Path targetPath = folderPath.resolve(filename);

            // Assegurar que o diretório existe
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            // Salvar o arquivo
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Construir URL de download - pegar caminho relativo completo a partir do diretório PSB
            String serverPath = folder.getServerPath();
            String relativePath;

            // Extrair o caminho relativo a partir de "/psb/" ou "\psb\"
            int psbIndex = serverPath.indexOf(File.separator + "psb" + File.separator);
            if (psbIndex != -1) {
                // Pega tudo depois de "/psb/"
                relativePath = serverPath.substring(psbIndex + 5); // +5 para pular "/psb/"
            } else {
                // Fallback: usar apenas o último segmento (comportamento antigo)
                relativePath = "dam-" + folder.getDam().getId() + "/"
                        + serverPath.substring(serverPath.lastIndexOf(File.separator) + 1);
            }

            String downloadUrl = baseUrl + "psb/" + relativePath + "/" + filename;

            // Salvar entidade no banco
            PSBFileEntity psbFile = new PSBFileEntity();
            psbFile.setFilename(filename);
            psbFile.setOriginalFilename(originalFilename);
            psbFile.setContentType(file.getContentType());
            psbFile.setSize(file.getSize());
            psbFile.setPsbFolder(folder);
            psbFile.setUploadedBy(uploader);
            psbFile.setFilePath(targetPath.toString());
            psbFile.setDownloadUrl(downloadUrl);

            return psbFileRepository.save(psbFile);

        } catch (IOException ex) {
            throw new FileStorageException("Não foi possível armazenar o arquivo.", ex);
        }
    }

    public Resource downloadFile(Long fileId) {
        try {
            PSBFileEntity file = psbFileRepository.findById(fileId)
                    .orElseThrow(() -> new NotFoundException("Arquivo PSB não encontrado"));

            Path filePath = Paths.get(file.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Não foi possível ler o arquivo");
            }

        } catch (IOException ex) {
            throw new FileStorageException("Não foi possível ler o arquivo", ex);
        }
    }

    @Transactional
    public void deleteFile(Long fileId) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getEditPSB()) {
                throw new NotFoundException("Usuário não tem permissão para excluir arquivos PSB");
            }
        }

        PSBFileEntity file = psbFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Arquivo PSB não encontrado"));

        try {
            Path filePath = Paths.get(file.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println("Erro ao excluir arquivo do sistema: " + ex.getMessage());
        }

        psbFileRepository.delete(file);
    }
}
