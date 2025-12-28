package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - DangerLevelEntity")
class DangerLevelEntityTest extends BaseUnitTest {

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
    }

    @Test
    @DisplayName("Should create danger level with all fields")
    void shouldCreateDangerLevelWithAllFields() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setId(1L);
        dangerLevel.setName("Alto Risco");
        dangerLevel.setDescription("Nível de perigo alto, requer atenção imediata");

        // Then
        assertThat(dangerLevel).satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getName()).isEqualTo("Alto Risco");
            assertThat(d.getDescription()).isEqualTo("Nível de perigo alto, requer atenção imediata");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        DangerLevelEntity dangerLevel = new DangerLevelEntity(
                1L,
                "Médio Risco",
                "Nível de perigo médio"
        );

        // Then
        assertThat(dangerLevel.getId()).isEqualTo(1L);
        assertThat(dangerLevel.getName()).isEqualTo("Médio Risco");
        assertThat(dangerLevel.getDescription()).isEqualTo("Nível de perigo médio");
    }

    @Test
    @DisplayName("Should enforce unique name constraint concept")
    void shouldEnforceUniqueNameConstraintConcept() {
        // Given
        DangerLevelEntity dangerLevel1 = new DangerLevelEntity();
        dangerLevel1.setId(1L);
        dangerLevel1.setName("Alto Risco");

        DangerLevelEntity dangerLevel2 = new DangerLevelEntity();
        dangerLevel2.setId(2L);
        dangerLevel2.setName("Alto Risco");

        // Then - In database, this would violate unique constraint
        assertThat(dangerLevel1.getName()).isEqualTo(dangerLevel2.getName());
        assertThat(dangerLevel1.getId()).isNotEqualTo(dangerLevel2.getId());
    }

    @Test
    @DisplayName("Should handle different danger level types")
    void shouldHandleDifferentDangerLevelTypes() {
        // Given
        DangerLevelEntity baixo = new DangerLevelEntity();
        baixo.setName("Baixo Risco");

        DangerLevelEntity medio = new DangerLevelEntity();
        medio.setName("Médio Risco");

        DangerLevelEntity alto = new DangerLevelEntity();
        alto.setName("Alto Risco");

        DangerLevelEntity critico = new DangerLevelEntity();
        critico.setName("Risco Crítico");

        // Then
        assertThat(baixo.getName()).contains("Baixo");
        assertThat(medio.getName()).contains("Médio");
        assertThat(alto.getName()).contains("Alto");
        assertThat(critico.getName()).contains("Crítico");
    }

    @Test
    @DisplayName("Should handle description updates")
    void shouldHandleDescriptionUpdates() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setDescription("Descrição inicial");

        // When
        dangerLevel.setDescription("Descrição atualizada com mais detalhes");

        // Then
        assertThat(dangerLevel.getDescription()).isEqualTo("Descrição atualizada com mais detalhes");
    }

    @Test
    @DisplayName("Should allow null description")
    void shouldAllowNullDescription() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setName("Baixo Risco");
        dangerLevel.setDescription(null);

        // Then
        assertThat(dangerLevel.getName()).isNotNull();
        assertThat(dangerLevel.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should handle long descriptions")
    void shouldHandleLongDescriptions() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        String longDescription = "Este nível de perigo representa uma situação de alto risco que requer atenção imediata e medidas preventivas específicas para garantir a segurança da estrutura e das pessoas envolvidas.";
        dangerLevel.setDescription(longDescription);

        // Then
        assertThat(dangerLevel.getDescription()).isEqualTo(longDescription);
        assertThat(dangerLevel.getDescription().length()).isGreaterThan(100);
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setName("Nível I - Baixo");

        // Then
        assertThat(dangerLevel.getName())
                .contains("-")
                .contains("I");
    }

    @Test
    @DisplayName("Should handle accents in name")
    void shouldHandleAccentsInName() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setName("Atenção");

        // Then
        assertThat(dangerLevel.getName()).contains("ç").contains("ã");
    }

    @Test
    @DisplayName("Should handle numeric classifications")
    void shouldHandleNumericClassifications() {
        // Given
        DangerLevelEntity level1 = new DangerLevelEntity();
        level1.setName("Nível 1");

        DangerLevelEntity level2 = new DangerLevelEntity();
        level2.setName("Nível 2");

        DangerLevelEntity level3 = new DangerLevelEntity();
        level3.setName("Nível 3");

        // Then
        assertThat(level1.getName()).contains("1");
        assertThat(level2.getName()).contains("2");
        assertThat(level3.getName()).contains("3");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setId(1L);
        dangerLevel.setName("Original Name");

        Long originalId = dangerLevel.getId();

        // When
        dangerLevel.setName("Updated Name");
        dangerLevel.setDescription("New description");

        // Then
        assertThat(dangerLevel.getId()).isEqualTo(originalId);
        assertThat(dangerLevel.getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Should handle color-based nomenclatures")
    void shouldHandleColorBasedNomenclatures() {
        // Given
        DangerLevelEntity green = new DangerLevelEntity();
        green.setName("Verde - Normal");

        DangerLevelEntity yellow = new DangerLevelEntity();
        yellow.setName("Amarelo - Atenção");

        DangerLevelEntity red = new DangerLevelEntity();
        red.setName("Vermelho - Alerta");

        // Then
        assertThat(green.getName()).contains("Verde");
        assertThat(yellow.getName()).contains("Amarelo");
        assertThat(red.getName()).contains("Vermelho");
    }

    @Test
    @DisplayName("Should support international nomenclatures")
    void shouldSupportInternationalNomenclatures() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setName("Low Risk");
        dangerLevel.setDescription("Low level of danger");

        // Then
        assertThat(dangerLevel.getName()).isEqualTo("Low Risk");
        assertThat(dangerLevel.getDescription()).contains("Low level");
    }

    @Test
    @DisplayName("Should handle danger level with empty description")
    void shouldHandleDangerLevelWithEmptyDescription() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setName("Baixo Risco");
        dangerLevel.setDescription("");

        // Then
        assertThat(dangerLevel.getName()).isNotEmpty();
        assertThat(dangerLevel.getDescription()).isEmpty();
    }

    @Test
    @DisplayName("Should handle alphanumeric danger level names")
    void shouldHandleAlphanumericDangerLevelNames() {
        // Given
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setName("Classe A1 - Alto Risco");

        // Then
        assertThat(dangerLevel.getName())
                .contains("A1")
                .contains("Classe");
    }
}
