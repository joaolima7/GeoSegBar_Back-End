package com.geosegbar.infra.file_storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.exceptions.FileStorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    /**
     * Arquivos maiores que 10MB usam multipart upload paralelo. O S3 exige que
     * cada parte (exceto a última) tenha no mínimo 5MB.
     */
    private static final long MULTIPART_THRESHOLD = 10 * 1024 * 1024L;
    private static final int PART_SIZE = 8 * 1024 * 1024; // 8MB por parte
    private static final int MAX_CONCURRENT_PARTS = 4;

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
            log.info("[S3 UPLOAD] Iniciando upload: bucket='{}', key='{}', tamanho={} bytes ({}MB)",
                    bucketName, s3Key, fileSizeBytes, String.format("%.1f", fileSizeBytes / (1024.0 * 1024.0)));
            long start = System.currentTimeMillis();

            if (fileSizeBytes > MULTIPART_THRESHOLD) {
                uploadMultipart(file, s3Key, fileSizeBytes);
            } else {
                PutObjectRequest putOb = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .build();
                s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), fileSizeBytes));
            }

            long elapsed = System.currentTimeMillis() - start;
            double mbPerSec = elapsed > 0 ? (fileSizeBytes / (1024.0 * 1024.0)) / (elapsed / 1000.0) : 0;
            log.info("[S3 UPLOAD] Upload concluido: key='{}', tempo={}ms ({} MB/s)",
                    s3Key, elapsed, String.format("%.2f", mbPerSec));

            return getS3Url(s3Key);

        } catch (IOException ex) {
            log.error("[S3 UPLOAD] IOException ao enviar para S3: {} | arquivo='{}'", ex.getMessage(), file.getOriginalFilename());
            throw new FileStorageException("Erro ao enviar arquivo para o S3.", ex);
        }
    }

    /**
     * Upload multipart paralelo para arquivos grandes. Divide o arquivo em
     * partes de 8MB e envia até 4 partes simultaneamente em conexões TCP
     * separadas, utilizando melhor a largura de banda. Típicamente 2-4x mais
     * rápido que um PUT único para arquivos >10MB.
     */
    private void uploadMultipart(MultipartFile file, String s3Key, long fileSize) throws IOException {
        byte[] fileBytes = file.getBytes();
        int partCount = (int) Math.ceil((double) fileSize / PART_SIZE);

        log.info("[S3 MULTIPART] Iniciando: key='{}', partes={}, tamanhoParte={}MB, threadsParalelas={}",
                s3Key, partCount, PART_SIZE / (1024 * 1024), Math.min(partCount, MAX_CONCURRENT_PARTS));

        String uploadId = null;
        try {
            CreateMultipartUploadRequest createReq = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();
            uploadId = s3Client.createMultipartUpload(createReq).uploadId();

            List<CompletedPart> completedParts = new CopyOnWriteArrayList<>();
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(partCount, MAX_CONCURRENT_PARTS));
            List<Future<?>> futures = new ArrayList<>();

            final String fUploadId = uploadId;
            for (int i = 0; i < partCount; i++) {
                final int partNumber = i + 1;
                final int offset = i * PART_SIZE;
                final int length = (int) Math.min(PART_SIZE, fileSize - offset);

                futures.add(executor.submit(() -> {
                    UploadPartRequest req = UploadPartRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .uploadId(fUploadId)
                            .partNumber(partNumber)
                            .contentLength((long) length)
                            .build();

                    UploadPartResponse resp = s3Client.uploadPart(req,
                            RequestBody.fromInputStream(
                                    new ByteArrayInputStream(fileBytes, offset, length), length));

                    completedParts.add(CompletedPart.builder()
                            .partNumber(partNumber)
                            .eTag(resp.eTag())
                            .build());

                    log.debug("[S3 MULTIPART] Parte {}/{} enviada ({} bytes)", partNumber, partCount, length);
                }));
            }

            for (Future<?> f : futures) {
                f.get();
            }
            executor.shutdown();

            completedParts.sort(Comparator.comparingInt(CompletedPart::partNumber));

            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build());

            log.info("[S3 MULTIPART] Concluido com sucesso: key='{}', partes={}", s3Key, partCount);

        } catch (Exception ex) {
            if (uploadId != null) {
                try {
                    s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .uploadId(uploadId)
                            .build());
                    log.info("[S3 MULTIPART] Upload abortado: key='{}'", s3Key);
                } catch (Exception abortEx) {
                    log.warn("[S3 MULTIPART] Falha ao abortar multipart upload: {}", abortEx.getMessage());
                }
            }
            if (ex instanceof IOException ioEx) {
                throw ioEx;
            }
            throw new FileStorageException("Erro no upload multipart para o S3.", ex);
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
