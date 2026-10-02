package com.geosegbar.unit.infra.checklist_response.services;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.enums.AnomalyOriginEnum;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.infra.checklist_response.services.EditedOtherAnomalyFactory;
import com.geosegbar.infra.checklist_submission.dtos.OtherSubmissionDTO;

@DisplayName("EditedOtherAnomalyFactory")
class EditedOtherAnomalyFactoryTest extends BaseUnitTest {

    private static final LocalDateTime ABRIL = LocalDateTime.of(2026, 4, 15, 14, 0);

    private static ChecklistResponseEntity response() {
        DamEntity dam = new DamEntity();
        dam.setId(3L);
        UserEntity user = new UserEntity();
        user.setId(8L);

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setId(77L);
        response.setCreatedAt(ABRIL);
        response.setDam(dam);
        response.setUser(user);
        return response;
    }

    private static OtherSubmissionDTO other() {
        OtherSubmissionDTO dto = new OtherSubmissionDTO();
        dto.setObservation("Trinca nova no talude");
        dto.setRecommendation("Monitorar");
        dto.setLatitude(-23.5);
        dto.setLongitude(-46.6);
        dto.setPhotos(List.of());
        return dto;
    }

    private static DangerLevelEntity dangerLevel() {
        DangerLevelEntity level = new DangerLevelEntity();
        level.setId(2L);
        return level;
    }

    private static AnomalyStatusEntity status() {
        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setId(1L);
        return status;
    }

    @Test
    @DisplayName("o apontamento criado na edição fica ligado à resposta")
    void linksToResponse() {
        AnomalyEntity anomaly = EditedOtherAnomalyFactory.build(
                other(), response(), dangerLevel(), status());

        assertThat(anomaly.getChecklistResponseId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("herda a data da inspeção, não a data da edição")
    void inheritsInspectionDate() {
        AnomalyEntity anomaly = EditedOtherAnomalyFactory.build(
                other(), response(), dangerLevel(), status());

        assertThat(anomaly.getCreatedAt()).isEqualTo(ABRIL);
    }

    @Test
    @DisplayName("nasce como 'Outros': origem CHECKLIST e sem pergunta vinculada")
    void bornAsOther() {
        AnomalyEntity anomaly = EditedOtherAnomalyFactory.build(
                other(), response(), dangerLevel(), status());

        assertThat(anomaly.getOrigin()).isEqualTo(AnomalyOriginEnum.CHECKLIST);
        assertThat(anomaly.getQuestionId()).isNull();
    }

    @Test
    @DisplayName("leva barragem, usuário, texto e coordenadas da resposta e do apontamento")
    void carriesContextAndContent() {
        AnomalyEntity anomaly = EditedOtherAnomalyFactory.build(
                other(), response(), dangerLevel(), status());

        assertThat(anomaly.getDam().getId()).isEqualTo(3L);
        assertThat(anomaly.getUser().getId()).isEqualTo(8L);
        assertThat(anomaly.getObservation()).isEqualTo("Trinca nova no talude");
        assertThat(anomaly.getRecommendation()).isEqualTo("Monitorar");
        assertThat(anomaly.getLatitude()).isEqualTo(-23.5);
        assertThat(anomaly.getLongitude()).isEqualTo(-46.6);
    }
}
