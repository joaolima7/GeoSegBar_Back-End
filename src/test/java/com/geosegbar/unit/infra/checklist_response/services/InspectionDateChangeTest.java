package com.geosegbar.unit.infra.checklist_response.services;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.checklist_response.services.InspectionDateChange;

@DisplayName("InspectionDateChange")
class InspectionDateChangeTest extends BaseUnitTest {

    private static final LocalDateTime OUTUBRO = LocalDateTime.of(2026, 10, 2, 9, 0);
    private static final LocalDateTime ABRIL = LocalDateTime.of(2026, 4, 15, 14, 0);

    private static AnomalyEntity anomalyAt(LocalDateTime createdAt) {
        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setCreatedAt(createdAt);
        return anomaly;
    }

    @Test
    @DisplayName("recusa data de inspeção no futuro")
    void rejectsFutureDate() {
        assertThatThrownBy(() -> InspectionDateChange.requireNotFuture(LocalDateTime.now().plusDays(1)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    @DisplayName("aceita data passada e data nula")
    void acceptsPastAndNull() {
        assertThatCode(() -> InspectionDateChange.requireNotFuture(ABRIL)).doesNotThrowAnyException();
        assertThatCode(() -> InspectionDateChange.requireNotFuture(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("as anomalias acompanham a mudança preservando o deslocamento entre elas")
    void shiftsAnomaliesKeepingRelativeOrder() {
        AnomalyEntity cinco = anomalyAt(OUTUBRO.plusMinutes(5));
        AnomalyEntity doze = anomalyAt(OUTUBRO.plusMinutes(12));

        InspectionDateChange.shiftAnomalies(List.of(cinco, doze), OUTUBRO, ABRIL);

        assertThat(cinco.getCreatedAt()).isEqualTo(ABRIL.plusMinutes(5));
        assertThat(doze.getCreatedAt()).isEqualTo(ABRIL.plusMinutes(12));
    }

    @Test
    @DisplayName("anomalia sem data não é tocada")
    void leavesAnomalyWithoutDateAlone() {
        AnomalyEntity semData = anomalyAt(null);

        InspectionDateChange.shiftAnomalies(List.of(semData), OUTUBRO, ABRIL);

        assertThat(semData.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("sem data anterior não há deslocamento a aplicar")
    void doesNothingWithoutPreviousDate() {
        AnomalyEntity anomaly = anomalyAt(OUTUBRO);

        InspectionDateChange.shiftAnomalies(List.of(anomaly), null, ABRIL);

        assertThat(anomaly.getCreatedAt()).isEqualTo(OUTUBRO);
    }

    @Test
    @DisplayName("o deslocamento nunca joga a anomalia para o futuro")
    void neverShiftsAnomalyIntoTheFuture() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime respostaAntiga = agora.minusDays(31);
        AnomalyEntity anomaly = anomalyAt(respostaAntiga.plusMinutes(14));

        InspectionDateChange.shiftAnomalies(List.of(anomaly), respostaAntiga, agora.minusMinutes(1));

        assertThat(anomaly.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
