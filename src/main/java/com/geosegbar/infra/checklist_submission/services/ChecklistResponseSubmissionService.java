package com.geosegbar.infra.checklist_submission.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistResponseSubmissionDTO;
import com.geosegbar.infra.file_storage.FileStorageService;

@Service
public class ChecklistResponseSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(ChecklistResponseSubmissionService.class);

    public record PendingPhotoUpload(
        Long entityId,
        boolean isAnomaly,
        byte[] imageBytes,
        String fileName,
        String contentType,
        String subDirectory,
        Long damId
    ) {}

    public record SubmissionResult(
        ChecklistResponseEntity checklistResponse,
        List<PendingPhotoUpload> pendingUploads
    ) {}

    private final ChecklistSubmissionPersistenceService persistenceService;
    private final FileStorageService fileStorageService;
    private final Executor checklistPhotoUploadExecutor;

    public ChecklistResponseSubmissionService(
            ChecklistSubmissionPersistenceService persistenceService,
            FileStorageService fileStorageService,
            @Qualifier("checklistPhotoUploadExecutor") Executor checklistPhotoUploadExecutor) {
        this.persistenceService = persistenceService;
        this.fileStorageService = fileStorageService;
        this.checklistPhotoUploadExecutor = checklistPhotoUploadExecutor;
    }

    public ChecklistResponseEntity submitChecklistResponse(ChecklistResponseSubmissionDTO submissionDto) {
        SubmissionResult result = persistenceService.persistChecklistData(submissionDto);

        if (!result.pendingUploads().isEmpty()) {
            processPhotoUploadsAsync(result.pendingUploads());
        }

        return result.checklistResponse();
    }

    private void processPhotoUploadsAsync(List<PendingPhotoUpload> pendingUploads) {
        List<CompletableFuture<Void>> futures = pendingUploads.stream()
                .map(pending -> CompletableFuture.runAsync(() -> {
                    try {
                        String url = fileStorageService.storeFileFromBytes(
                                pending.imageBytes(),
                                pending.fileName(),
                                pending.contentType(),
                                pending.subDirectory()
                        );
                        if (pending.isAnomaly()) {
                            persistenceService.updateAnomalyPhotoPath(pending.entityId(), url);
                        } else {
                            persistenceService.updateAnswerPhotoPath(pending.entityId(), url);
                        }
                    } catch (Exception e) {
                        log.error("[CHECKLIST-PHOTO] Falha ao fazer upload da foto id={} isAnomaly={}: {}",
                                pending.entityId(), pending.isAnomaly(), e.getMessage());
                    }
                }, checklistPhotoUploadExecutor))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(90, TimeUnit.SECONDS)
                    .join();
        } catch (Exception e) {
            log.error("[CHECKLIST-PHOTO] Timeout ou erro aguardando uploads: {}", e.getMessage());
        }
    }
}
