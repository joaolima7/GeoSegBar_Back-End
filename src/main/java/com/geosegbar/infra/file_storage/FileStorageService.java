package com.geosegbar.infra.file_storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
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
    private final S3Presigner s3Presigner;

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

    /**
     * Prazo de validade da URL de download. Generoso de propósito: cobre o
     * INÍCIO do download, não a duração dele — uma vez que o S3 aceitou a
     * requisição, a transferência continua mesmo depois de a URL expirar.
     */
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(30);

    /**
     * URL pré-assinada de GET, para o cliente baixar direto do S3.
     *
     * Substitui o padrão anterior, em que o Spring abria a própria conexão com
     * o S3 e repassava os bytes ao cliente. Aquilo fazia o arquivo trafegar em
     * dobro (S3 -> Spring -> cliente), prendia uma thread do Tomcat pela
     * transferência inteira e ainda esbarrava no proxy_read_timeout do nginx:
     * 6,1 GB a 100 Mbps levam cerca de 8 minutos, e o limite era de 5.
     *
     * É o mesmo mecanismo que o upload já usa desde sempre, na direção
     * contrária — o download é que não tinha equivalente.
     *
     * responseContentDisposition e responseContentType são sobrescritos na
     * assinatura: sem eles o navegador receberia o nome interno do objeto no
     * S3 (com o timestamp que o upload prefixa) em vez do nome original.
     *
     * @param fileUrl URL do objeto como está gravada no banco
     * @param downloadFilename nome que o navegador deve dar ao arquivo
     * @param contentType tipo a devolver; nulo cai no que o S3 tem gravado
     */
    public String generatePresignedDownloadUrl(String fileUrl, String downloadFilename, String contentType) {
        String key = extractKeyFromUrl(fileUrl);
        if (key == null) {
            throw new FileStorageException("URL inválida para download: " + fileUrl);
        }

        GetObjectRequest.Builder getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key);

        if (downloadFilename != null && !downloadFilename.isBlank()) {
            getRequest.responseContentDisposition(contentDisposition(downloadFilename));
        }

        if (contentType != null && !contentType.isBlank()) {
            getRequest.responseContentType(contentType);
        }

        try {
            String url = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(DOWNLOAD_URL_TTL)
                    .getObjectRequest(getRequest.build())
                    .build())
                    .url().toString();

            log.info("[S3 PRESIGNED GET] URL gerada: key='{}', validade={}min", key, DOWNLOAD_URL_TTL.toMinutes());
            return url;

        } catch (Exception ex) {
            throw new FileStorageException("Erro ao gerar URL de download para: " + key, ex);
        }
    }

    /**
     * Content-Disposition no formato do RFC 6266: um filename ASCII como
     * reserva e um filename* com o nome real.
     *
     * Montado à mão em vez de usar o ContentDisposition do Spring porque ele
     * codifica o filename simples como palavra-codificada do RFC 2047
     * (=?UTF-8?Q?...?=), que navegador nenhum entende nesse campo. Com nome de
     * arquivo acentuado — "Inspeção de Rotina.pdf", o caso comum aqui — o
     * usuário receberia esse rótulo como nome do arquivo em qualquer cliente
     * que não leia o filename*.
     */
    private String contentDisposition(String filename) {
        String ascii = filename
                .replaceAll("[^\\x20-\\x7E]", "_")
                .replace("\\", "_")
                .replace("\"", "_");

        String utf8 = java.net.URLEncoder
                .encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");

        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
    }

    /**
     * Abre o objeto do S3 para leitura em fluxo, sem trazer o arquivo inteiro
     * para a memória.
     *
     * Use este método sempre que o destino for outro fluxo (resposta HTTP, ZIP).
     * {@link #downloadFileBytes(String)} carrega tudo de uma vez e só serve para
     * arquivos comprovadamente pequenos — foi o que derrubou a aplicação em
     * 24/08/2026, ao montar o ZIP de uma pasta compartilhada.
     *
     * Quem chama é responsável por fechar o stream.
     */
    public java.io.InputStream openStream(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        if (key == null) {
            throw new FileStorageException("URL inválida para download: " + fileUrl);
        }
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            return s3Client.getObject(getReq);
        } catch (Exception ex) {
            throw new FileStorageException("Erro ao abrir arquivo no S3: " + key, ex);
        }
    }

    /**
     * Carrega o arquivo inteiro na memória.
     *
     * CUIDADO: um arquivo de PSB pode ter até 512 MB. Para copiar de um fluxo
     * para outro, use {@link #openStream(String)}.
     */
    public byte[] downloadFileBytes(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        if (key == null) {
            throw new FileStorageException("URL inválida para download: " + fileUrl);
        }
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try (var stream = s3Client.getObject(getReq)) {
            return stream.readAllBytes();
        } catch (Exception ex) {
            throw new FileStorageException("Erro ao baixar arquivo do S3: " + key, ex);
        }
    }

    public void overwriteFile(String existingUrl, byte[] newBytes, String contentType) {
        String key = extractKeyFromUrl(existingUrl);
        if (key == null) {
            throw new FileStorageException("URL inválida para sobrescrever: " + existingUrl);
        }
        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(putOb, RequestBody.fromBytes(newBytes));
        log.info("[S3 OVERWRITE] Arquivo sobrescrito: key='{}'", key);
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

    /**
     * Reconstrói a URL pública final de um objeto a partir da sua chave S3.
     * Mesma forma de URL usada pelos uploads existentes ({@link #getS3Url}),
     * garantindo consistência de exibição. Usado pelo fluxo presigned, onde o
     * servidor NUNCA confia numa URL vinda do cliente — só na chave (objectKey).
     */
    public String publicUrlForKey(String key) {
        return getS3Url(key);
    }

    /**
     * Verifica se um objeto existe no bucket (HEAD). Usado pelo fluxo presigned
     * para confirmar, antes de persistir, que a imagem foi de fato enviada ao
     * S3. Retorna {@code false} se a chave não existir; propaga erros de
     * infraestrutura (para não persistir referências não validadas).
     */
    public boolean objectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
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
