package com.geosegbar.infra.psb.services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.geosegbar.infra.psb.dtos.PresignedUploadCompleteRequest.CompletedPartDTO;
import com.geosegbar.infra.psb.dtos.PresignedUploadInitResponse;
import com.geosegbar.infra.psb.dtos.PresignedUploadInitResponse.PresignedPartUrl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

/**
 * Serviço para gerar URLs pré-assinadas de upload multipart direto ao S3.
 *
 * TODOS os uploads usam multipart — sem caminho "single PUT". Isto simplifica o
 * fluxo do cliente e garante consistência.
 *
 * Part sizing dinâmico: - Arquivos até 50MB → 1 parte (mín 5MB do S3 = OK) -
 * Arquivos até 500MB → partes de 50MB - Arquivos > 500MB → partes de 100MB
 *
 * Fluxo: 1. initUpload() → cria multipart no S3, gera URLs pré-assinadas por
 * parte 2. (cliente envia cada parte diretamente ao S3 usando as URLs) 3.
 * completeUpload() → completa multipart no S3, retorna URL final 4.
 * abortUpload() → cancela multipart e limpa metadados
 *
 * Expiracoes: URLs assinadas: 15min | Upload pendente: 30min | Cleanup: 5min.
 * Startup cleanup: aborta automaticamente uploads orphaos via
 * ListMultipartUploads (garante limpeza mesmo apos restart da API, quando o
 * estado em memoria e perdido).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresignedUploadService {

    /**
     * Parte padrão para arquivos médios: 50MB
     */
    private static final long PART_SIZE_MEDIUM = 50 * 1024 * 1024L;

    /**
     * Parte para arquivos grandes (>500MB): 100MB
     */
    private static final long PART_SIZE_LARGE = 100 * 1024 * 1024L;

    /**
     * Limiar para usar partes grandes
     */
    private static final long LARGE_FILE_THRESHOLD = 500 * 1024 * 1024L;

    /**
     * URLs pré-assinadas válidas por 15 minutos. Suficiente para uploads
     * grandes (ex: 500MB a ~5MB/s ≈ 100s).
     */
    private static final Duration PRESIGN_EXPIRATION = Duration.ofMinutes(15);

    /**
     * Uploads pendentes expiram após 30 minutos. Se não confirmar em 30min,
     * algo deu errado — abortar no S3.
     */
    private static final Duration UPLOAD_EXPIRATION = Duration.ofMinutes(30);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * Armazena metadados de uploads pendentes (em memória). Chave: uploadId
     * (UUID gerado pelo Spring)
     */
    private final Map<String, PendingUpload> pendingUploads = new ConcurrentHashMap<>();

    // =========================================================================
    // INIT — Cria multipart upload + gera URLs pré-assinadas
    // =========================================================================
    /**
     * Inicializa upload multipart pré-assinado. SEMPRE multipart — simplifica
     * fluxo do cliente, funciona para qualquer tamanho.
     *
     * @param s3Key chave S3 (ex: "psb/folder/timestamp_file.pdf")
     * @param fileSize tamanho em bytes
     * @param contentType MIME type
     * @param folderId ID da pasta PSB
     * @param uploadedById ID do usuário
     * @return resposta com URLs pré-assinadas por parte
     */
    public PresignedUploadInitResponse initUpload(String s3Key, long fileSize, String contentType,
            Long folderId, Long uploadedById) {
        long startMs = System.nanoTime();

        // Part size dinâmico: arquivos grandes usam partes maiores
        long partSize = calculatePartSize(fileSize);
        int totalParts = Math.max(1, (int) Math.ceil((double) fileSize / partSize));

        // Cria multipart upload no S3
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String s3UploadId = createResponse.uploadId();

        // Gera URL pré-assinada para cada parte
        List<PresignedPartUrl> parts = new ArrayList<>(totalParts);
        for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .uploadId(s3UploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartPresignRequest presignPartRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(PRESIGN_EXPIRATION)
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            PresignedUploadPartRequest presignedPart = s3Presigner.presignUploadPart(presignPartRequest);

            parts.add(PresignedPartUrl.builder()
                    .partNumber(partNumber)
                    .uploadUrl(presignedPart.url().toString())
                    .build());
        }

        // Registra upload pendente (in-memory)
        String uploadId = java.util.UUID.randomUUID().toString();
        PendingUpload pending = new PendingUpload(
                uploadId, s3Key, fileSize, contentType, folderId, uploadedById,
                s3UploadId, totalParts, Instant.now()
        );
        pendingUploads.put(uploadId, pending);

        long elapsedMs = (System.nanoTime() - startMs) / 1_000_000;
        log.info("[PRESIGNED] Init multipart: uploadId={}, s3UploadId={}, s3Key={}, "
                + "tamanho={}MB, partes={}, partSize={}MB, tempo={}ms",
                uploadId, s3UploadId, s3Key,
                String.format("%.1f", fileSize / (1024.0 * 1024.0)),
                totalParts,
                String.format("%.0f", partSize / (1024.0 * 1024.0)),
                elapsedMs);

        return PresignedUploadInitResponse.builder()
                .uploadId(uploadId)
                .s3Key(s3Key)
                .s3UploadId(s3UploadId)
                .partSize(partSize)
                .totalParts(totalParts)
                .parts(parts)
                .multipart(true)
                .build();
    }

    /**
     * Calcula tamanho ideal da parte baseado no tamanho do arquivo. S3 exige
     * mínimo 5MB por parte (exceto última).
     */
    private long calculatePartSize(long fileSize) {
        if (fileSize >= LARGE_FILE_THRESHOLD) {
            return PART_SIZE_LARGE; // 100MB para arquivos >500MB
        }
        if (fileSize > PART_SIZE_MEDIUM) {
            return PART_SIZE_MEDIUM; // 50MB para arquivos médios
        }
        // Para arquivos pequenos: usar o arquivo inteiro como 1 parte
        // (S3 permite min 5MB, e a última parte pode ser menor)
        return Math.max(fileSize, 5 * 1024 * 1024L); // mín 5MB
    }

    // =========================================================================
    // COMPLETE — Confirma upload multipart
    // =========================================================================
    /**
     * Confirma que o upload multipart direto ao S3 foi concluído. Chama
     * CompleteMultipartUpload no S3 com as ETags das partes.
     *
     * @return dados do arquivo para salvar no banco
     */
    public CompleteResult completeUpload(String uploadId, List<CompletedPartDTO> completedParts) {
        long startMs = System.nanoTime();

        PendingUpload pending = pendingUploads.remove(uploadId);
        if (pending == null) {
            throw new IllegalArgumentException(
                    "Upload não encontrado ou expirado: " + uploadId);
        }

        if (completedParts == null || completedParts.isEmpty()) {
            abortMultipartUpload(pending.s3Key(), pending.s3UploadId());
            throw new IllegalArgumentException(
                    "Lista de partes completadas é obrigatória. Esperado: " + pending.totalParts() + " partes");
        }

        // Monta lista de partes S3 (sorted by partNumber)
        List<CompletedPart> s3Parts = completedParts.stream()
                .sorted((a, b) -> Integer.compare(a.getPartNumber(), b.getPartNumber()))
                .map(part -> CompletedPart.builder()
                .partNumber(part.getPartNumber())
                .eTag(part.getETag())
                .build())
                .toList();

        // Completa multipart upload no S3
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(pending.s3Key())
                .uploadId(pending.s3UploadId())
                .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(s3Parts)
                        .build())
                .build();

        s3Client.completeMultipartUpload(completeRequest);

        // Gera URL pública
        String downloadUrl = getS3Url(pending.s3Key());

        long elapsedMs = (System.nanoTime() - startMs) / 1_000_000;
        long totalElapsed = Duration.between(pending.createdAt(), Instant.now()).toMillis();

        log.info("[PRESIGNED] Complete: uploadId={}, s3Key={}, tamanho={}MB, "
                + "partes={}, tempoComplete={}ms, tempoTotal={}ms",
                uploadId, pending.s3Key(),
                String.format("%.1f", pending.fileSize() / (1024.0 * 1024.0)),
                s3Parts.size(), elapsedMs, totalElapsed);

        return new CompleteResult(
                pending.s3Key(),
                downloadUrl,
                pending.fileSize(),
                pending.contentType(),
                pending.folderId(),
                pending.uploadedById()
        );
    }

    // =========================================================================
    // ABORT — Cancela upload
    // =========================================================================
    /**
     * Cancela um upload pendente. Se multipart, aborta no S3.
     */
    public void abortUpload(String uploadId) {
        PendingUpload pending = pendingUploads.remove(uploadId);
        if (pending == null) {
            log.warn("[PRESIGNED] Tentativa de cancelar upload não encontrado: {}", uploadId);
            return;
        }

        if (pending.s3UploadId() != null) {
            abortMultipartUpload(pending.s3Key(), pending.s3UploadId());
        }

        log.info("[PRESIGNED] Upload cancelado: uploadId={}, s3Key={}", uploadId, pending.s3Key());
    }

    private void abortMultipartUpload(String s3Key, String s3UploadId) {
        try {
            AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .uploadId(s3UploadId)
                    .build();
            s3Client.abortMultipartUpload(abortRequest);
            log.info("[PRESIGNED] Multipart upload abortado no S3: key={}, uploadId={}", s3Key, s3UploadId);
        } catch (Exception e) {
            log.error("[PRESIGNED] Erro ao abortar multipart upload: key={}, uploadId={}: {}",
                    s3Key, s3UploadId, e.getMessage());
        }
    }

    // =========================================================================
    // CLEANUP — Remove uploads expirados (scheduler + startup)
    // =========================================================================
    /**
     * Ao iniciar a API: aborta no S3 todos os multipart uploads incompletos que
     * foram iniciados há mais de UPLOAD_EXPIRATION.
     *
     * Isso resolve o problema de restart: quando a API reinicia, o estado em
     * memória é perdido. Sem este cleanup, os uploads multipart ficariam
     * pendentes no S3 para sempre, acumulando fragmentos cobráveis.
     *
     * Usa ListMultipartUploads do S3 — independente da memória local.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOrphanedMultipartUploadsOnStartup() {
        Instant cutoff = Instant.now().minus(UPLOAD_EXPIRATION);
        int aborted = 0;

        try {
            ListMultipartUploadsResponse listResponse = s3Client.listMultipartUploads(
                    ListMultipartUploadsRequest.builder().bucket(bucketName).build());

            for (MultipartUpload upload : listResponse.uploads()) {
                if (upload.initiated() != null && upload.initiated().isBefore(cutoff)) {
                    abortMultipartUpload(upload.key(), upload.uploadId());
                    aborted++;
                }
            }

            if (aborted > 0) {
                log.warn("[PRESIGNED] Startup: {} multipart upload(s) orphao(s) abortado(s) (>{}min)",
                        aborted, UPLOAD_EXPIRATION.toMinutes());
            } else {
                log.info("[PRESIGNED] Startup: nenhum upload orphao encontrado no S3");
            }
        } catch (Exception e) {
            log.error("[PRESIGNED] Erro no startup cleanup de multipart uploads: {}", e.getMessage());
        }
    }

    /**
     * A cada 5 minutos, varre a memória e aborta no S3 uploads que passaram de
     * UPLOAD_EXPIRATION sem ser confirmados.
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredUploads() {
        Instant cutoff = Instant.now().minus(UPLOAD_EXPIRATION);
        int cleaned = 0;

        for (var entry : pendingUploads.entrySet()) {
            PendingUpload pending = entry.getValue();
            if (pending.createdAt().isBefore(cutoff)) {
                pendingUploads.remove(entry.getKey());

                if (pending.s3UploadId() != null) {
                    abortMultipartUpload(pending.s3Key(), pending.s3UploadId());
                }

                cleaned++;
                log.warn("[PRESIGNED] Upload expirado (>{}min) removido: uploadId={}, s3Key={}, criadoEm={}",
                        UPLOAD_EXPIRATION.toMinutes(), entry.getKey(), pending.s3Key(), pending.createdAt());
            }
        }

        if (cleaned > 0) {
            log.info("[PRESIGNED] Cleanup: {} upload(s) expirado(s) removido(s)", cleaned);
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private String getS3Url(String key) {
        return s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()).toExternalForm();
    }

    /**
     * Retorna info sobre uploads pendentes (para diagnóstico).
     */
    public int getPendingUploadCount() {
        return pendingUploads.size();
    }

    // =========================================================================
    // RECORDS
    // =========================================================================
    /**
     * Metadados de um upload pendente
     */
    record PendingUpload(
            String uploadId,
            String s3Key,
            long fileSize,
            String contentType,
            Long folderId,
            Long uploadedById,
            String s3UploadId,
            int totalParts,
            Instant createdAt
            ) {

    }

    /**
     * Resultado de um upload confirmado
     */
    public record CompleteResult(
            String s3Key,
            String downloadUrl,
            long fileSize,
            String contentType,
            Long folderId,
            Long uploadedById
            ) {

    }
}
