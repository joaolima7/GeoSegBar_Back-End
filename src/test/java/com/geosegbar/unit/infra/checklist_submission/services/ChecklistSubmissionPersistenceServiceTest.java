package com.geosegbar.unit.infra.checklist_submission.services;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.QuestionnaireResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.services.ChecklistSubmissionPersistenceService;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

/**
 * Validações de entrada da submissão de checklist: usuário e barragem precisam
 * existir antes de qualquer coisa ser gravada. Essas checagens moram aqui desde
 * que a persistência foi extraída de ChecklistResponseSubmissionService (que
 * agora só delega) — o que este teste cobre são os dois NotFound que antes eram
 * testados na fachada. As demais dependências do serviço ficam nulas de
 * propósito: as duas exceções saem antes de qualquer uso delas.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("ChecklistSubmissionPersistenceService Unit Tests - Simplified")
class ChecklistSubmissionPersistenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DamRepository damRepository;

    @Mock
    private ChecklistResponseRepository checklistResponseRepository;

    @InjectMocks
    private ChecklistSubmissionPersistenceService service;

    private ChecklistResponseSubmissionDTO submissionDTO;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found")
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findByIdWithPermissions(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.persistChecklistData(submissionDTO));

        assertEquals("Usuário não encontrado!", exception.getMessage());
        verify(userRepository).findByIdWithPermissions(1L);
        verifyNoInteractions(damRepository);
        verify(checklistResponseRepository, never()).save(any(ChecklistResponseEntity.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when dam not found")
    void shouldThrowNotFoundExceptionWhenDamNotFound() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(1L);

        when(userRepository.findByIdWithPermissions(1L)).thenReturn(Optional.of(userEntity));
        when(damRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.persistChecklistData(submissionDTO));

        assertEquals("Barragem não encontrada!", exception.getMessage());
        verify(damRepository).findById(1L);
        verify(checklistResponseRepository, never()).save(any(ChecklistResponseEntity.class));
    }
}
