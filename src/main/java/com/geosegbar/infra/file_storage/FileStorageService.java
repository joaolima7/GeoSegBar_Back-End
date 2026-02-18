package com.geosegbar.infra.file_storage;

import java.io.IOException;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.exceptions.FileStorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";

            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String safeFileName = timestamp + "_" + (originalFileName != null
                    ? originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_") : "file" + fileExtension);

            String s3Key = subDirectory + "/" + safeFileName;

            long fileSizeBytes = file.getSize();
            log.info("[S3 UPLOAD] Iniciando PUT: bucket='{}', key='{}', tamanho={} bytes",
                    bucketName, s3Key, fileSizeBytes);
            long start = System.currentTimeMillis();

            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), fileSizeBytes));

            long elapsed = System.currentTimeMillis() - start;
            log.info("[S3 UPLOAD] PUT concluido: key='{}', tempo={}ms", s3Key, elapsed);

            return getS3Url(s3Key);

        } catch (IOException ex) {
            log.error("[S3 UPLOAD] IOException ao enviar para S3: {} | arquivo='{}'", ex.getMessage(), file.getOriginalFilename());
            throw new FileStorageException("Erro ao enviar arquivo para o S3.", ex);
        }
    }

    public String storeFileFromBytes(byte[] fileBytes, String originalFileName, String contentType, String subDirectory) {
        try {
            String fileExtension = "";

            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            } else if (contentType != null) {
                if (contentType.contains("jpeg")) {
                    fileExtension = ".jpg";
                } else if (contentType.contains("png")) {
                    fileExtension = ".png";
                }

            }

            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String safeFileName = timestamp + "_" + (originalFileName != null
                    ? originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_") : "file" + fileExtension);

            String s3Key = subDirectory + "/" + safeFileName;

            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putOb, RequestBody.fromBytes(fileBytes));

            return getS3Url(s3Key);

        } catch (Exception ex) {
            throw new FileStorageException("Erro ao enviar bytes para o S3.", ex);
        }
    }

    public void deleteFile(String fileUrl) {
        try {

            String fileKey = extractKeyFromUrl(fileUrl);

            if (fileKey != null) {
                DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .build();
                s3Client.deleteObject(deleteReq);
                log.info("Arquivo deletado do S3: {}", fileKey);
            }
        } catch (Exception ex) {
            log.error("Erro ao deletar arquivo do S3: {}", ex.getMessage());

        }
    }

    private String getS3Url(String key) {
        return s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()).toExternalForm();
    }

    private String extractKeyFromUrl(String fileUrl) {

        try {

            if (fileUrl.contains(bucketName)) {

                String splitToken = ".amazonaws.com/";
                if (fileUrl.contains(splitToken)) {
                    return fileUrl.split(splitToken)[1];
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
