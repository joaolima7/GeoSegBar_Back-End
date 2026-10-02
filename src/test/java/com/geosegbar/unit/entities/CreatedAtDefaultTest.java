package com.geosegbar.unit.entities;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.ChecklistResponseEntity;

/**
 * As duas entidades deixaram de usar @CreationTimestamp para que a data da
 * inspeção possa ser apontada à mão. Sem uma rede de segurança, qualquer
 * caminho de criação que esqueça de atribuir a data grava nulo.
 */
@DisplayName("Data de criação quando ninguém atribui")
class CreatedAtDefaultTest extends BaseUnitTest {

    @Test
    @DisplayName("anomalia sem data recebe o instante atual ao ser persistida")
    void anomalyWithoutDateGetsNow() {
        AnomalyEntity anomaly = new AnomalyEntity();
        LocalDateTime antes = LocalDateTime.now();

        anomaly.applyCreatedAtDefault();

        assertThat(anomaly.getCreatedAt()).isNotNull();
        assertThat(anomaly.getCreatedAt()).isAfterOrEqualTo(antes);
    }

    @Test
    @DisplayName("anomalia com data retroativa mantém a data informada")
    void anomalyKeepsExplicitDate() {
        AnomalyEntity anomaly = new AnomalyEntity();
        LocalDateTime abril = LocalDateTime.of(2026, 4, 15, 14, 0);
        anomaly.setCreatedAt(abril);

        anomaly.applyCreatedAtDefault();

        assertThat(anomaly.getCreatedAt()).isEqualTo(abril);
    }

    @Test
    @DisplayName("resposta de checklist sem data recebe o instante atual")
    void checklistResponseWithoutDateGetsNow() {
        ChecklistResponseEntity response = new ChecklistResponseEntity();
        LocalDateTime antes = LocalDateTime.now();

        response.applyCreatedAtDefault();

        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getCreatedAt()).isAfterOrEqualTo(antes);
    }

    @Test
    @DisplayName("resposta de checklist com data retroativa mantém a data informada")
    void checklistResponseKeepsExplicitDate() {
        ChecklistResponseEntity response = new ChecklistResponseEntity();
        LocalDateTime abril = LocalDateTime.of(2026, 4, 15, 14, 0);
        response.setCreatedAt(abril);

        response.applyCreatedAtDefault();

        assertThat(response.getCreatedAt()).isEqualTo(abril);
    }
}
