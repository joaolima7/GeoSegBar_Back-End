package com.geosegbar.unit.infra.anomaly.services;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.anomaly.dtos.UpdateAnomalyRequestDTO;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.anomaly.services.AnomalyService;
import com.geosegbar.infra.anomaly_photo.persistence.jpa.AnomalyPhotoRepository;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.dam.services.DamAccessService;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.template_questionnaire_question.persistence.jpa.TemplateQuestionnaireQuestionRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

@DisplayName("AnomalyService — vínculo com pergunta do checklist")
class AnomalyServiceQuestionLinkTest extends BaseUnitTest {

    @Mock
    private AnomalyRepository anomalyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DamRepository damRepository;
    @Mock
    private DangerLevelRepository dangerLevelRepository;
    @Mock
    private AnomalyStatusRepository statusRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AnomalyPhotoRepository anomalyPhotoRepository;
    @Mock
    private DamAccessService damAccessService;
    @Mock
    private TemplateQuestionnaireQuestionRepository templateQuestionnaireQuestionRepository;

    @InjectMocks
    private AnomalyService anomalyService;

    private AnomalyEntity anomalyWithQuestionnaire(Long questionnaireId) {
        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setId(1L);
        anomaly.setQuestionnaireId(questionnaireId);
        lenient().when(anomalyRepository.findById(1L)).thenReturn(Optional.of(anomaly));
        lenient().when(anomalyRepository.save(any(AnomalyEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        return anomaly;
    }

    @Test
    @DisplayName("vincula a uma pergunta do próprio questionário")
    void linksAnomalyToQuestion() {
        AnomalyEntity anomaly = anomalyWithQuestionnaire(10L);
        when(templateQuestionnaireQuestionRepository
                .findByTemplateQuestionnaireIdAndQuestionId(10L, 55L))
                .thenReturn(Optional.of(new TemplateQuestionnaireQuestionEntity()));

        UpdateAnomalyRequestDTO request = new UpdateAnomalyRequestDTO();
        request.setQuestionId(55L);

        anomalyService.update(1L, request);

        assertThat(anomaly.getQuestionId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("recusa pergunta que não pertence ao questionário da anomalia")
    void rejectsQuestionFromAnotherQuestionnaire() {
        anomalyWithQuestionnaire(10L);
        when(templateQuestionnaireQuestionRepository
                .findByTemplateQuestionnaireIdAndQuestionId(10L, 999L))
                .thenReturn(Optional.empty());

        UpdateAnomalyRequestDTO request = new UpdateAnomalyRequestDTO();
        request.setQuestionId(999L);

        assertThatThrownBy(() -> anomalyService.update(1L, request))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("não pertence");

        verify(anomalyRepository, never()).save(any());
    }

    @Test
    @DisplayName("questionId zero desvincula e devolve a anomalia para Outros")
    void unlinksAnomalyFromQuestion() {
        AnomalyEntity anomaly = anomalyWithQuestionnaire(10L);
        anomaly.setQuestionId(55L);

        UpdateAnomalyRequestDTO request = new UpdateAnomalyRequestDTO();
        request.setQuestionId(0L);

        anomalyService.update(1L, request);

        assertThat(anomaly.getQuestionId()).isNull();
    }

    @Test
    @DisplayName("recusa vincular quando a anomalia não pertence a questionário nenhum")
    void rejectsLinkWhenAnomalyHasNoQuestionnaire() {
        anomalyWithQuestionnaire(null);

        UpdateAnomalyRequestDTO request = new UpdateAnomalyRequestDTO();
        request.setQuestionId(55L);

        assertThatThrownBy(() -> anomalyService.update(1L, request))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("questionário");
    }

    @Test
    @DisplayName("atualizar só a observação continua funcionando")
    void stillUpdatesObservationAlone() {
        AnomalyEntity anomaly = anomalyWithQuestionnaire(10L);

        UpdateAnomalyRequestDTO request = new UpdateAnomalyRequestDTO();
        request.setObservation("Trinca reaberta");

        anomalyService.update(1L, request);

        assertThat(anomaly.getObservation()).isEqualTo("Trinca reaberta");
        assertThat(anomaly.getQuestionId()).isNull();
    }
}
