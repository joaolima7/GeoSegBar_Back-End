package com.geosegbar.infra.file_storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.exceptions.FileStorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:target/uploads}")
    private String uploadDir;
    
    @Value("${file.base-url:http://backend.geometrisa-prod.com.br:9090/uploads/}")
    private String baseUrl;

    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            Path uploadPath = Paths.get(uploadDir + "/" + subDirectory).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String fileName = UUID.randomUUID() + fileExtension;
            
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return baseUrl + subDirectory + "/" + fileName;
            
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file.", ex);
        }
    }

    public String storeFileFromBytes(byte[] fileBytes, String originalFileName, String contentType, String subDirectory) {
        try {
            Path uploadPath = Paths.get(uploadDir + "/" + subDirectory).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            } else if (contentType != null) {
                switch (contentType) {
                    case "image/jpeg": fileExtension = ".jpg"; break;
                    case "image/png": fileExtension = ".png"; break;
                    case "image/gif": fileExtension = ".gif"; break;
                    case "image/bmp": fileExtension = ".bmp"; break;
                    default: fileExtension = "";
                }
            }
            
            String fileName = UUID.randomUUID() + fileExtension;
            
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(new ByteArrayInputStream(fileBytes), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return baseUrl + subDirectory + "/" + fileName;
            
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file from bytes.", ex);
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
            throw new FileStorageException("Could not delete file.", ex);
        }
    }
}