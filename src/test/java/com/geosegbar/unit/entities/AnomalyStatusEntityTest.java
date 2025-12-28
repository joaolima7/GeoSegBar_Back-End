package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnomalyStatusEntity;

@DisplayName("Unit Tests - AnomalyStatusEntity")
class AnomalyStatusEntityTest extends BaseUnitTest {

    @Test
    @DisplayName("Should create anomaly status with all fields")
    void shouldCreateAnomalyStatusWithAllFields() {

        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setId(1L);
        status.setName("Aberta");
        status.setDescription("Anomalia identificada e registrada");

        assertThat(status).satisfies(s -> {
            assertThat(s.getId()).isEqualTo(1L);
            assertThat(s.getName()).isEqualTo("Aberta");
            assertThat(s.getDescription()).isEqualTo("Anomalia identificada e registrada");
        });
    }

    @Test
    @DisplayName("Should create anomaly status using all args constructor")
    void shouldCreateAnomalyStatusUsingAllArgsConstructor() {

        AnomalyStatusEntity status = new AnomalyStatusEntity(
                1L,
                "Em Análise",
                "Anomalia em processo de análise técnica"
        );

        assertThat(status).satisfies(s -> {
            assertThat(s.getId()).isEqualTo(1L);
            assertThat(s.getName()).isEqualTo("Em Análise");
            assertThat(s.getDescription()).isEqualTo("Anomalia em processo de análise técnica");
        });
    }

    @Test
    @DisplayName("Should create anomaly status with no args constructor")
    void shouldCreateAnomalyStatusWithNoArgsConstructor() {

        AnomalyStatusEntity status = new AnomalyStatusEntity();

        assertThat(status).isNotNull();
        assertThat(status.getId()).isNull();
        assertThat(status.getName()).isNull();
        assertThat(status.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should allow null description")
    void shouldAllowNullDescription() {

        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setId(1L);
        status.setName("Fechada");
        status.setDescription(null);

        assertThat(status.getName()).isEqualTo("Fechada");
        assertThat(status.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should handle different status types")
    void shouldHandleDifferentStatusTypes() {

        AnomalyStatusEntity opened = new AnomalyStatusEntity(1L, "Aberta", "Nova anomalia");
        AnomalyStatusEntity inAnalysis = new AnomalyStatusEntity(2L, "Em Análise", "Sendo analisada");
        AnomalyStatusEntity inProgress = new AnomalyStatusEntity(3L, "Em Andamento", "Correção em andamento");
        AnomalyStatusEntity resolved = new AnomalyStatusEntity(4L, "Resolvida", "Anomalia corrigida");
        AnomalyStatusEntity closed = new AnomalyStatusEntity(5L, "Fechada", "Anomalia finalizada");

        assertThat(opened.getName()).isEqualTo("Aberta");
        assertThat(inAnalysis.getName()).isEqualTo("Em Análise");
        assertThat(inProgress.getName()).isEqualTo("Em Andamento");
        assertThat(resolved.getName()).isEqualTo("Resolvida");
        assertThat(closed.getName()).isEqualTo("Fechada");
    }

    @Test
    @DisplayName("Should maintain name uniqueness constraint concept")
    void shouldMaintainNameUniquenessConstraintConcept() {

        AnomalyStatusEntity status1 = new AnomalyStatusEntity();
        status1.setId(1L);
        status1.setName("Aberta");

        AnomalyStatusEntity status2 = new AnomalyStatusEntity();
        status2.setId(2L);
        status2.setName("Aberta");

        assertThat(status1.getName()).isEqualTo(status2.getName());
        assertThat(status1.getId()).isNotEqualTo(status2.getId());
    }

    @Test
    @DisplayName("Should handle status with long description")
    void shouldHandleStatusWithLongDescription() {

        String longDescription = "Descrição muito longa que contém muitos detalhes sobre o status da anomalia, "
                + "incluindo critérios de transição, responsabilidades, prazos e outras informações relevantes "
                + "para o processo de gestão de anomalias. ".repeat(5);

        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setName("Status Complexo");
        status.setDescription(longDescription);

        assertThat(status.getDescription()).hasSize(longDescription.length());
        assertThat(status.getDescription()).contains("critérios de transição");
    }

    @Test
    @DisplayName("Should support status name update")
    void shouldSupportStatusNameUpdate() {

        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setId(1L);
        status.setName("Aberta");

        status.setName("Em Análise");

        assertThat(status.getName()).isEqualTo("Em Análise");
    }

    @Test
    @DisplayName("Should support status description update")
    void shouldSupportStatusDescriptionUpdate() {

        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setId(1L);
        status.setName("Aberta");
        status.setDescription("Descrição inicial");

        status.setDescription("Descrição atualizada com mais detalhes");

        assertThat(status.getDescription()).isEqualTo("Descrição atualizada com mais detalhes");
    }

    @Test
    @DisplayName("Should handle status names with special characters")
    void shouldHandleStatusNamesWithSpecialCharacters() {

        AnomalyStatusEntity status1 = new AnomalyStatusEntity();
        status1.setName("Em Análise - Aguardando");

        AnomalyStatusEntity status2 = new AnomalyStatusEntity();
        status2.setName("Fechada (Não Resolvida)");

        assertThat(status1.getName()).contains("-");
        assertThat(status2.getName()).contains("(").contains(")");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setId(1L);
        status.setName("Aberta");
        status.setDescription("Descrição inicial");

        Long originalId = status.getId();

        status.setName("Em Análise");
        status.setDescription("Nova descrição");

        assertThat(status.getId()).isEqualTo(originalId);
        assertThat(status.getName()).isEqualTo("Em Análise");
        assertThat(status.getDescription()).isEqualTo("Nova descrição");
    }

    @Test
    @DisplayName("Should handle empty description")
    void shouldHandleEmptyDescription() {

        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setName("Aberta");
        status.setDescription("");

        assertThat(status.getDescription()).isEmpty();
    }
}
