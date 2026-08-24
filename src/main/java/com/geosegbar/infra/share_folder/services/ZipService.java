package com.geosegbar.infra.share_folder.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.infra.file_storage.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZipService {

    private final FileStorageService fileStorageService;

    public ByteArrayOutputStream createZipFromFolder(PSBFolderEntity folder) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            Set<String> addedPaths = new HashSet<>();

            addFilesToZip(folder, "", zos, addedPaths);

            if (addedPaths.isEmpty()) {
                // Melhor um erro claro do que um .zip vazio que o usuário só descobre
                // que está vazio depois de baixar.
                throw new FileStorageException(
                        "A pasta '" + folder.getName() + "' não possui arquivos para download.");
            }

            zos.finish();
            log.info("ZIP criado com {} arquivo(s) para a pasta: {}", addedPaths.size(), folder.getName());

        } catch (IOException e) {
            log.error("Erro ao criar ZIP para pasta {}: {}", folder.getName(), e.getMessage());
            throw new FileStorageException("Não foi possível criar o arquivo ZIP", e);
        }

        return baos;
    }

    private void addFilesToZip(PSBFolderEntity folder, String pathPrefix,
            ZipOutputStream zos, Set<String> addedPaths) throws IOException {

        String currentPath = pathPrefix.isEmpty() ? folder.getName() : pathPrefix + "/" + folder.getName();

        if (folder.getFiles() != null && !folder.getFiles().isEmpty()) {
            for (PSBFileEntity file : folder.getFiles()) {
                addFileToZip(file, currentPath, zos, addedPaths);
            }
        }

        if (folder.getSubfolders() != null && !folder.getSubfolders().isEmpty()) {
            for (PSBFolderEntity subfolder : folder.getSubfolders()) {
                addFilesToZip(subfolder, currentPath, zos, addedPaths);
            }
        }
    }

    private void addFileToZip(PSBFileEntity file, String folderPath,
            ZipOutputStream zos, Set<String> addedPaths) throws IOException {

        String zipEntryPath = folderPath + "/" + file.getOriginalFilename();

        if (addedPaths.contains(zipEntryPath)) {
            log.warn("Arquivo duplicado ignorado: {}", zipEntryPath);
            return;
        }

        // Os arquivos vivem no S3: filePath guarda a CHAVE do objeto, não um caminho
        // de disco. A versão anterior fazia Paths.get(filePath) + Files.exists(), que
        // é sempre falso dentro do container — e o "return" silencioso fazia o ZIP
        // sair vazio, com HTTP 200 e sem nenhum aviso ao usuário.
        byte[] conteudo;
        try {
            conteudo = fileStorageService.downloadFileBytes(file.getDownloadUrl());
        } catch (RuntimeException e) {
            log.error("Falha ao baixar do S3 o arquivo '{}' (url={}): {}",
                    file.getOriginalFilename(), file.getDownloadUrl(), e.getMessage());
            throw new IOException("Falha ao ler o arquivo " + file.getOriginalFilename() + " do storage", e);
        }

        try {
            ZipEntry zipEntry = new ZipEntry(zipEntryPath);
            zipEntry.setSize(conteudo.length);
            zos.putNextEntry(zipEntry);

            zos.write(conteudo);

            zos.closeEntry();
            addedPaths.add(zipEntryPath);

            log.debug("Arquivo adicionado ao ZIP: {}", zipEntryPath);

        } catch (IOException e) {
            log.error("Erro ao adicionar arquivo {} ao ZIP: {}", file.getOriginalFilename(), e.getMessage());
            throw e;
        }
    }
}
