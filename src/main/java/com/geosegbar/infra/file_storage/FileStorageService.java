package com.geosegbar.infra.file_storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.exceptions.FileStorageException;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.base-url}")
    private String baseUrl;

    @Value("${application.frontend-url}")
    private String frontendUrl;

    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            Path uploadPath = Paths.get(uploadDir + "/" + subDirectory).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";

            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String safeFileName = timestamp + "_" + (originalFileName != null
                    ? originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_") : "file" + fileExtension);

            Path targetLocation = uploadPath.resolve(safeFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return frontendUrl + baseUrl + subDirectory + "/" + safeFileName;
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
                    case "image/jpeg":
                        fileExtension = ".jpg";
                        break;
                    case "image/png":
                        fileExtension = ".png";
                        break;
                    case "image/gif":
                        fileExtension = ".gif";
                        break;
                    case "image/bmp":
                        fileExtension = ".bmp";
                        break;
                    default:
                        fileExtension = "";
                }
            }

            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String safeFileName = timestamp + "_" + (originalFileName != null
                    ? originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_") : "file" + fileExtension);

            Path targetLocation = uploadPath.resolve(safeFileName);
            Files.copy(new ByteArrayInputStream(fileBytes), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return frontendUrl + baseUrl + subDirectory + "/" + safeFileName;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file from bytes.", ex);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String urlWithoutDomain = fileUrl;
            if (fileUrl != null && fileUrl.startsWith(frontendUrl)) {
                urlWithoutDomain = fileUrl.substring(frontendUrl.length());
            }

            if (urlWithoutDomain != null && urlWithoutDomain.startsWith(baseUrl)) {
                String relativePath = urlWithoutDomain.substring(baseUrl.length());
                Path filePath = Paths.get(uploadDir + "/" + relativePath).toAbsolutePath().normalize();
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file.", ex);
        }
    }
}
