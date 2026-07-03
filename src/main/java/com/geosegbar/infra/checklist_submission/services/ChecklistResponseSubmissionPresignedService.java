package com.geosegbar.infra.checklist_submission.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.OtherSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.QuestionnaireResponseSubmissionDTO;

/**
 * Orquestra a submissão de checklist no fluxo PRESIGNED (imagens já no S3).
 * <p>
 * Passos: (1) coletar as fotos da árvore; (2) validar cada {@code objectKey}
 * (não-branco, prefixo permitido, existente no S3 — HEADs em paralelo),
 * fail-fast antes de tocar o banco; (3) persistir tudo numa única transação
 * (tudo-ou-nada) já com as URLs finais; (4) confirmar as chaves (deixam de ser
 * candidatas a limpeza de órfão). As validações de negócio são idênticas às do
 * submit base64 (reusadas em {@code persistChecklistDataPresigned}).
 */
@Service
public class ChecklistResponseSubmissionPresignedService {

    private static final Logger log = LoggerFactory.getLogger(ChecklistResponseSubmissionPresignedService.class);

    private final ChecklistSubmissionPersistenceService persistenceService;
    private final ChecklistPhotoPresignService presignService;
    private final Executor checklistPhotoUploadExecutor;

    public ChecklistResponseSubmissionPresignedService(
            ChecklistSubmissionPersistenceService persistenceService,
            ChecklistPhotoPresignService presignService,
            @Qualifier("checklistPhotoUploadExecutor") Executor checklistPhotoUploadExecutor) {
        this.persistenceService = persistenceService;
        this.presignService = presignService;
        this.checklistPhotoUploadExecutor = checklistPhotoUploadExecutor;
    }

    public ChecklistResponseEntity submitChecklistResponse(ChecklistResponseSubmissionDTO submissionDto) {
        List<PhotoSubmissionDTO> photos = collectPhotos(submissionDto);

        // 1. objectKey obrigatório + prefixo permitido
        for (PhotoSubmissionDTO photo : photos) {
            String key = photo.getObjectKey();
            if (key == null || key.isBlank()) {
                throw new InvalidInputException(
                        "Toda foto deve conter 'objectKey' (chave S3 do upload pré-assinado).");
            }
            if (!presignService.isAllowedKey(key)) {
                throw new InvalidInputException("objectKey inválido (prefixo não permitido): " + key);
            }
        }

        // 2. Existência no S3 (HEAD em paralelo), fail-fast — chaves distintas
        List<String> distinctKeys = photos.stream()
                .map(PhotoSubmissionDTO::getObjectKey)
                .distinct()
                .toList();

        if (!distinctKeys.isEmpty()) {
            Map<String, Boolean> existence = new ConcurrentHashMap<>();
            List<CompletableFuture<Void>> checks = distinctKeys.stream()
                    .map(key -> CompletableFuture.runAsync(
                            () -> existence.put(key, presignService.objectExists(key)),
                            checklistPhotoUploadExecutor))
                    .toList();
            try {
                CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
                        .orTimeout(60, TimeUnit.SECONDS)
                        .join();
            } catch (Exception e) {
                throw new InvalidInputException("Falha ao validar as imagens no S3: " + e.getMessage());
            }

            List<String> missing = distinctKeys.stream()
                    .filter(key -> !Boolean.TRUE.equals(existence.get(key)))
                    .toList();
            if (!missing.isEmpty()) {
                throw new InvalidInputException(
                        "Imagens não encontradas no S3. Envie todas as imagens (PUT) antes de submeter. "
                        + "Chaves ausentes: " + missing);
            }
        }

        // 3. Mapa objectKey → URL final (reconstruída server-side)
        Map<String, String> urlByObjectKey = new HashMap<>();
        for (String key : distinctKeys) {
            urlByObjectKey.put(key, presignService.publicUrl(key));
        }

        // 4. Persistência transacional (tudo-ou-nada)
        ChecklistResponseEntity saved = persistenceService.persistChecklistDataPresigned(submissionDto, urlByObjectKey);

        // 5. Chaves confirmadas — não são mais órfãs
        presignService.confirmKeys(distinctKeys);

        log.info("[CHECKLIST-PRESIGN] Checklist {} submetido com {} imagem(ns).", saved.getId(), distinctKeys.size());
        return saved;
    }

    private List<PhotoSubmissionDTO> collectPhotos(ChecklistResponseSubmissionDTO dto) {
        List<PhotoSubmissionDTO> photos = new ArrayList<>();
        if (dto.getQuestionnaireResponses() != null) {
            for (QuestionnaireResponseSubmissionDTO q : dto.getQuestionnaireResponses()) {
                if (q.getAnswers() != null) {
                    for (AnswerSubmissionDTO a : q.getAnswers()) {
                        if (a.getPhotos() != null) {
                            photos.addAll(a.getPhotos());
                        }
                    }
                }
                if (q.getOthers() != null) {
                    for (OtherSubmissionDTO o : q.getOthers()) {
                        if (o.getPhotos() != null) {
                            photos.addAll(o.getPhotos());
                        }
                    }
                }
            }
        }
        if (dto.getOthers() != null) {
            for (OtherSubmissionDTO o : dto.getOthers()) {
                if (o.getPhotos() != null) {
                    photos.addAll(o.getPhotos());
                }
            }
        }
        return photos;
    }
}
