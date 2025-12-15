package com.geosegbar.infra.share_folder.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.exceptions.FileStorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZipService {

    public ByteArrayOutputStream createZipFromFolder(PSBFolderEntity folder) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            Set<String> addedPaths = new HashSet<>();

            addFilesToZip(folder, "", zos, addedPaths);

            zos.finish();
            log.info("ZIP criado com sucesso para pasta: {}", folder.getName());

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

        Path filePath = Paths.get(file.getFilePath());

        if (!Files.exists(filePath)) {
            log.error("Arquivo não encontrado no storage: {}", file.getFilePath());

            return;
        }

        try {
            ZipEntry zipEntry = new ZipEntry(zipEntryPath);
            zipEntry.setSize(file.getSize());
            zos.putNextEntry(zipEntry);

            Files.copy(filePath, zos);

            zos.closeEntry();
            addedPaths.add(zipEntryPath);

            log.debug("Arquivo adicionado ao ZIP: {}", zipEntryPath);

        } catch (IOException e) {
            log.error("Erro ao adicionar arquivo {} ao ZIP: {}", file.getOriginalFilename(), e.getMessage());
            throw e;
        }
    }
}
