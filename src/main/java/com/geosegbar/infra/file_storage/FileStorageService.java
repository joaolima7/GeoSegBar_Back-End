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

    // Removemos frontendUrl e uploadDir, não são mais necessários para salvar
    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";

            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // Gera nome único
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String safeFileName = timestamp + "_" + (originalFileName != null
                    ? originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_") : "file" + fileExtension);

            // Caminho completo no S3 (ex: answer-photos/123456_foto.jpg)
            String s3Key = subDirectory + "/" + safeFileName;

            // Prepara a requisição de upload
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    // .acl(ObjectCannedACL.PUBLIC_READ) // Descomente se o bucket não for público por padrão
                    .build();

            // Faz o upload enviando os bytes
            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Retorna a URL pública do arquivo
            return getS3Url(s3Key);

        } catch (IOException ex) {
            throw new FileStorageException("Erro ao enviar arquivo para o S3.", ex);
        }
    }

    public String storeFileFromBytes(byte[] fileBytes, String originalFileName, String contentType, String subDirectory) {
        try {
            String fileExtension = "";
            // ... (Sua lógica de extensão mantida igual) ...
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            } else if (contentType != null) {
                if (contentType.contains("jpeg")) {
                    fileExtension = ".jpg"; 
                }else if (contentType.contains("png")) {
                    fileExtension = ".png";
                }
                // ... resto da sua lógica
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

            // Upload a partir de Bytes
            s3Client.putObject(putOb, RequestBody.fromBytes(fileBytes));

            return getS3Url(s3Key);

        } catch (Exception ex) {
            throw new FileStorageException("Erro ao enviar bytes para o S3.", ex);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            // Lógica para extrair a "Key" (caminho relativo) da URL completa
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
            // Não lançamos erro aqui para não travar o processo principal
        }
    }

    // Auxiliar para pegar a URL
    private String getS3Url(String key) {
        return s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()).toExternalForm();
    }

    // Auxiliar para extrair a chave do S3 a partir da URL completa salva no banco
    private String extractKeyFromUrl(String fileUrl) {
        // Exemplo URL: https://meu-bucket.s3.amazonaws.com/logos/arquivo.jpg
        // Queremos apenas: logos/arquivo.jpg
        try {
            // Forma simples: pegar tudo depois do nome do bucket
            if (fileUrl.contains(bucketName)) {
                // Ajuste essa lógica conforme o formato exato da sua URL S3
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
