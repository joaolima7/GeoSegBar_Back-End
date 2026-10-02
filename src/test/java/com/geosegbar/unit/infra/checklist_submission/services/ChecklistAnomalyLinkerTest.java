package com.geosegbar.unit.infra.checklist_submission.services;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.infra.checklist_submission.services.ChecklistAnomalyLinker;

@DisplayName("ChecklistAnomalyLinker")
class ChecklistAnomalyLinkerTest extends BaseUnitTest {

    private static ChecklistResponseEntity response(Long id, LocalDateTime createdAt) {
        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setId(id);
        response.setCreatedAt(createdAt);
        return response;
    }

    @Test
    @DisplayName("guarda o id da resposta de checklist que gerou a anomalia")
    void keepsChecklistResponseId() {
        AnomalyEntity anomaly = new AnomalyEntity();

        ChecklistAnomalyLinker.link(anomaly, response(4321L, LocalDateTime.of(2026, 10, 2, 9, 0)));

        assertThat(anomaly.getChecklistResponseId()).isEqualTo(4321L);
    }

    @Test
    @DisplayName("a anomalia nasce com a data da inspeção, não com a data de hoje")
    void inheritsInspectionDate() {
        AnomalyEntity anomaly = new AnomalyEntity();
        LocalDateTime abril = LocalDateTime.of(2026, 4, 15, 14, 0);

        ChecklistAnomalyLinker.link(anomaly, response(1L, abril));

        assertThat(anomaly.getCreatedAt()).isEqualTo(abril);
    }

    @Test
    @DisplayName("sem data na resposta, a anomalia usa o instante atual")
    void fallsBackToNowWhenResponseHasNoDate() {
        AnomalyEntity anomaly = new AnomalyEntity();
        LocalDateTime antes = LocalDateTime.now();

        ChecklistAnomalyLinker.link(anomaly, response(1L, null));

        assertThat(anomaly.getCreatedAt()).isNotNull();
        assertThat(anomaly.getCreatedAt()).isAfterOrEqualTo(antes);
    }
}
