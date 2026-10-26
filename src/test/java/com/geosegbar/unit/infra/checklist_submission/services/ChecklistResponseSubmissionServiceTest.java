package com.geosegbar.unit.infra.checklist_submission.services;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.QuestionnaireResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.services.ChecklistResponseSubmissionService;
import com.geosegbar.infra.checklist_submission.services.ChecklistResponseSubmissionService.PendingPhotoUpload;
import com.geosegbar.infra.checklist_submission.services.ChecklistResponseSubmissionService.SubmissionResult;
import com.geosegbar.infra.checklist_submission.services.ChecklistSubmissionPersistenceService;
import com.geosegbar.infra.file_storage.FileStorageService;

/**
 * ChecklistResponseSubmissionService é hoje a fachada da submissão: a
 * persistência (e as validações de usuário/barragem) vive em
 * ChecklistSubmissionPersistenceService — ver ChecklistSubmissionPersistenceServiceTest
 * — e a fachada só delega e envia as fotos pendentes depois do commit.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("ChecklistResponseSubmissionService Unit Tests - Simplified")
class ChecklistResponseSubmissionServiceTest {

    @Mock
    private ChecklistSubmissionPersistenceService persistenceService;

    @Mock
    private FileStorageService fileStorageService;

    private ChecklistResponseSubmissionService service;

    private ChecklistResponseSubmissionDTO submissionDTO;
    private ChecklistResponseEntity checklistResponse;

    @BeforeEach
    void setUp() {
        // Executor síncrono: os uploads rodam na própria thread do teste.
        service = new ChecklistResponseSubmissionService(persistenceService, fileStorageService, Runnable::run);

        AnswerSubmissionDTO answerDTO = new AnswerSubmissionDTO();
        answerDTO.setQuestionId(1L);
        answerDTO.setComment("Test Answer");

        QuestionnaireResponseSubmissionDTO questionnaireDTO = new QuestionnaireResponseSubmissionDTO();
        questionnaireDTO.setTemplateQuestionnaireId(1L);
        questionnaireDTO.setAnswers(Arrays.asList(answerDTO));

        submissionDTO = new ChecklistResponseSubmissionDTO();
        submissionDTO.setDamId(1L);
        submissionDTO.setChecklistId(1L);
        submissionDTO.setChecklistName("Test Checklist");
        submissionDTO.setUserId(1L);
        submissionDTO.setQuestionnaireResponses(Arrays.asList(questionnaireDTO));
        submissionDTO.setMobile(false);

        checklistResponse = new ChecklistResponseEntity();
        checklistResponse.setId(10L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found")
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        when(persistenceService.persistChecklistData(submissionDTO))
                .thenThrow(new NotFoundException("Usuário não encontrado!"));

        assertThrows(NotFoundException.class, () -> service.submitChecklistResponse(submissionDTO));

        verify(persistenceService).persistChecklistData(submissionDTO);
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Should throw NotFoundException when dam not found")
    void shouldThrowNotFoundExceptionWhenDamNotFound() {
        when(persistenceService.persistChecklistData(submissionDTO))
                .thenThrow(new NotFoundException("Barragem não encontrada!"));

        assertThrows(NotFoundException.class, () -> service.submitChecklistResponse(submissionDTO));

        verify(persistenceService).persistChecklistData(submissionDTO);
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Should return persisted checklist response without touching storage when there are no pending photos")
    void shouldReturnChecklistResponseWhenThereAreNoPendingUploads() {
        when(persistenceService.persistChecklistData(submissionDTO))
                .thenReturn(new SubmissionResult(checklistResponse, List.of()));

        ChecklistResponseEntity result = service.submitChecklistResponse(submissionDTO);

        assertSame(checklistResponse, result);
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Should upload pending photos and update answer and anomaly photo paths")
    void shouldUploadPendingPhotosAndUpdatePaths() {
        byte[] answerBytes = new byte[]{1, 2, 3};
        byte[] anomalyBytes = new byte[]{4, 5, 6};
        PendingPhotoUpload answerPhoto = new PendingPhotoUpload(
                100L, false, answerBytes, "answer.png", "image/png", "answer-photos", 1L);
        PendingPhotoUpload anomalyPhoto = new PendingPhotoUpload(
                200L, true, anomalyBytes, "anomaly.png", "image/png", "anomaly-photos", 1L);

        when(persistenceService.persistChecklistData(submissionDTO))
                .thenReturn(new SubmissionResult(checklistResponse, List.of(answerPhoto, anomalyPhoto)));
        when(fileStorageService.storeFileFromBytes(answerBytes, "answer.png", "image/png", "answer-photos"))
                .thenReturn("https://files.test/answer-photos/answer.png");
        when(fileStorageService.storeFileFromBytes(anomalyBytes, "anomaly.png", "image/png", "anomaly-photos"))
                .thenReturn("https://files.test/anomaly-photos/anomaly.png");

        ChecklistResponseEntity result = service.submitChecklistResponse(submissionDTO);

        assertSame(checklistResponse, result);
        verify(persistenceService).updateAnswerPhotoPath(100L, "https://files.test/answer-photos/answer.png");
        verify(persistenceService).updateAnomalyPhotoPath(200L, "https://files.test/anomaly-photos/anomaly.png");
    }

    @Test
    @DisplayName("Should still return the checklist response when a photo upload fails")
    void shouldReturnChecklistResponseWhenPhotoUploadFails() {
        byte[] bytes = new byte[]{1, 2, 3};
        PendingPhotoUpload photo = new PendingPhotoUpload(
                100L, false, bytes, "answer.png", "image/png", "answer-photos", 1L);

        when(persistenceService.persistChecklistData(submissionDTO))
                .thenReturn(new SubmissionResult(checklistResponse, List.of(photo)));
        when(fileStorageService.storeFileFromBytes(bytes, "answer.png", "image/png", "answer-photos"))
                .thenThrow(new RuntimeException("S3 indisponível"));

        ChecklistResponseEntity result = service.submitChecklistResponse(submissionDTO);

        assertSame(checklistResponse, result);
        verify(persistenceService, never()).updateAnswerPhotoPath(any(), anyString());
        verify(persistenceService, never()).updateAnomalyPhotoPath(any(), anyString());
    }
}
