package com.geosegbar.infra.checklist_submission.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.geosegbar.infra.checklist_submission.dtos.ChecklistPhotoKind;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistPhotoPresignRequestDTO;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistPhotoPresignResponseDTO;
import com.geosegbar.infra.file_storage.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Gera URLs pré-assinadas (single-PUT) para o front subir as fotos do checklist
 * DIRETO ao S3, sem passar pelo backend. Espelha o padrão do PSB
 * ({@code PresignedUploadService}), porém em modo single-PUT — adequado a
 * imagens (≤ 5GB por PUT), muito mais simples para o cliente do que multipart
 * por arquivo quando há ~150 imagens.
 * <p>
 * As chaves usam os MESMOS prefixos do fluxo base64 ({@code answer-photos/} e
 * {@code anomalies/}), então a política de leitura do bucket já vale e as URLs
 * exibem igual às atuais.
 * <p>
 * Limpeza de órfãos: registra em memória cada chave assinada; um sweep periódico
 * apaga do S3 as que não foram "confirmadas" (submissão concluída) dentro do TTL
 * — cobre o caso de upload iniciado e checklist nunca submetido, ou submit
 * revertido. (Chaves órfãs anteriores a um restart não são varridas — vazamento
 * mínimo e barato; pode ser tratado por rotina de bucket futuramente.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistPhotoPresignService {

    private static final Duration PRESIGN_EXPIRATION = Duration.ofMinutes(15);
    private static final Duration ORPHAN_TTL = Duration.ofHours(2);

    public static final String ANSWER_PREFIX = "answer-photos";
    public static final String ANOMALY_PREFIX = "anomalies";

    private final S3Presigner s3Presigner;
    private final FileStorageService fileStorageService;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * Chaves assinadas aguardando confirmação (submissão). key → instante do presign.
     */
    private final Map<String, Instant> pendingKeys = new ConcurrentHashMap<>();

    /**
     * FASE 1 — gera uma URL PUT pré-assinada por imagem.
     */
    public ChecklistPhotoPresignResponseDTO presignBatch(ChecklistPhotoPresignRequestDTO request) {
        Instant now = Instant.now();
        List<ChecklistPhotoPresignResponseDTO.Item> out = new ArrayList<>(request.getItems().size());

        for (ChecklistPhotoPresignRequestDTO.Item item : request.getItems()) {
            String prefix = item.getKind() == ChecklistPhotoKind.ANOMALY ? ANOMALY_PREFIX : ANSWER_PREFIX;
            String objectKey = buildObjectKey(prefix, item.getFileName());

            PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey);
            // Se o content-type for informado, ele é ASSINADO — o front deve enviar
            // o mesmo header Content-Type no PUT, senão o S3 responde 403.
            if (item.getContentType() != null && !item.getContentType().isBlank()) {
                putBuilder.contentType(item.getContentType());
            }

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(PRESIGN_EXPIRATION)
                    .putObjectRequest(putBuilder.build())
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

            pendingKeys.put(objectKey, now);

            out.add(ChecklistPhotoPresignResponseDTO.Item.builder()
                    .clientRef(item.getClientRef())
                    .objectKey(objectKey)
                    .uploadUrl(presigned.url().toString())
                    .expiresAt(now.plus(PRESIGN_EXPIRATION))
                    .build());
        }

        log.info("[CHECKLIST-PRESIGN] {} URL(s) pré-assinada(s) geradas.", out.size());
        return new ChecklistPhotoPresignResponseDTO(out);
    }

    /**
     * Valida que a chave está sob um prefixo permitido (evita referência a
     * objetos fora dos prefixos de foto de checklist).
     */
    public boolean isAllowedKey(String objectKey) {
        return objectKey != null
                && (objectKey.startsWith(ANSWER_PREFIX + "/") || objectKey.startsWith(ANOMALY_PREFIX + "/"));
    }

    /**
     * Reconstrói (server-side) a URL pública final da chave. Nunca confiamos em
     * URL vinda do cliente — só na chave.
     */
    public String publicUrl(String objectKey) {
        return fileStorageService.publicUrlForKey(objectKey);
    }

    /**
     * Confirma no S3 (HEAD) que a imagem foi de fato enviada.
     */
    public boolean objectExists(String objectKey) {
        return fileStorageService.objectExists(objectKey);
    }

    /**
     * Marca chaves como confirmadas (submissão persistida) — deixam de ser
     * candidatas a limpeza de órfão.
     */
    public void confirmKeys(Collection<String> objectKeys) {
        if (objectKeys != null) {
            objectKeys.forEach(pendingKeys::remove);
        }
    }

    private String buildObjectKey(String prefix, String fileName) {
        String safeFileName = (fileName == null || fileName.isBlank())
                ? "file"
                : fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
        LocalDate today = LocalDate.now();
        String datePath = String.format("%04d/%02d/%02d",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        return prefix + "/" + datePath + "/" + UUID.randomUUID() + "_" + safeFileName;
    }

    /**
     * Sweep a cada 10 min: apaga do S3 as chaves assinadas mas não confirmadas
     * dentro do TTL (upload iniciado e checklist nunca submetido, ou revertido).
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupOrphanUploads() {
        Instant cutoff = Instant.now().minus(ORPHAN_TTL);
        int removed = 0;
        for (Map.Entry<String, Instant> entry : pendingKeys.entrySet()) {
            if (entry.getValue().isBefore(cutoff)) {
                String key = entry.getKey();
                pendingKeys.remove(key);
                try {
                    fileStorageService.deleteFile(fileStorageService.publicUrlForKey(key));
                    removed++;
                } catch (Exception e) {
                    log.warn("[CHECKLIST-PRESIGN] Falha ao apagar órfão key={}: {}", key, e.getMessage());
                }
            }
        }
        if (removed > 0) {
            log.info("[CHECKLIST-PRESIGN] Cleanup: {} upload(s) órfão(s) removido(s) (>{}h).",
                    removed, ORPHAN_TTL.toHours());
        }
    }
}
