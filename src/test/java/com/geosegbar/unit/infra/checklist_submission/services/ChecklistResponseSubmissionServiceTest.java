package com.geosegbar.unit.infra.checklist_submission.services;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.anomaly_photo.persistence.jpa.AnomalyPhotoRepository;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.answer_photo.persistence.jpa.AnswerPhotoRepository;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.QuestionnaireResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.services.ChecklistResponseSubmissionService;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;
import com.geosegbar.infra.documentation_dam.persistence.DocumentationDamRepository;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("ChecklistResponseSubmissionService Unit Tests - Simplified")
class ChecklistResponseSubmissionServiceTest {

    @Mock
    private ChecklistResponseRepository checklistResponseRepository;

    @Mock
    private QuestionnaireResponseRepository questionnaireResponseRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AnswerPhotoRepository answerPhotoRepository;

    @Mock
    private AnomalyRepository anomalyRepository;

    @Mock
    private AnomalyPhotoRepository anomalyPhotoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DamRepository damRepository;

    @Mock
    private ChecklistRepository checklistRepository;

    @Mock
    private TemplateQuestionnaireRepository templateQuestionnaireRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnomalyStatusRepository anomalyStatusRepository;

    @Mock
    private DangerLevelRepository dangerLevelRepository;

    @Mock
    private DocumentationDamRepository documentationDamRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private ChecklistResponseSubmissionService service;

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
        when(userRepository.findByIdWithClients(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.submitChecklistResponse(submissionDTO));
        verify(userRepository).findByIdWithClients(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when dam not found")
    void shouldThrowNotFoundExceptionWhenDamNotFound() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(1L);

        when(userRepository.findByIdWithClients(1L)).thenReturn(Optional.of(userEntity));
        when(damRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.submitChecklistResponse(submissionDTO));
        verify(damRepository).findById(1L);
    }
}
