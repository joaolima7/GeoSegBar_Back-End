package com.geosegbar.infra.file_storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.exceptions.FileStorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${file.base-url:http://localhost:8080/uploads/}")
    private String baseUrl;

    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            // Cria diretórios se não existirem
            Path uploadPath = Paths.get(uploadDir + "/" + subDirectory).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            
            // Gera nome único para o arquivo para evitar substituições
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String fileName = UUID.randomUUID() + fileExtension;
            
            // Salva o arquivo no sistema de arquivos
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Retorna a URL para o arquivo
            return baseUrl + subDirectory + "/" + fileName;
            
        } catch (IOException ex) {
            throw new FileStorageException("Não foi possível armazenar o arquivo.", ex);
        }
    }
    
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && fileUrl.startsWith(baseUrl)) {
                String relativePath = fileUrl.substring(baseUrl.length());
                Path filePath = Paths.get(uploadDir + "/" + relativePath).toAbsolutePath().normalize();
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ex) {
            throw new FileStorageException("Não foi possível excluir o arquivo.", ex);
        }
    }
}