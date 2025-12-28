package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClassificationDamEntity;
import com.geosegbar.entities.RegulatoryDamEntity;

@DisplayName("Unit Tests - ClassificationDamEntity")
class ClassificationDamEntityTest extends BaseUnitTest {

    @Test
    @DisplayName("Should create classification dam with all fields")
    void shouldCreateClassificationDamWithAllFields() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        classification.setId(1L);
        classification.setClassification("Pequeno Porte");

        // Then
        assertThat(classification).satisfies(c -> {
            assertThat(c.getId()).isEqualTo(1L);
            assertThat(c.getClassification()).isEqualTo("Pequeno Porte");
            assertThat(c.getRegulatoryDams()).isNotNull().isEmpty();
        });
    }

    @Test
    @DisplayName("Should create classification dam using all args constructor")
    void shouldCreateClassificationDamUsingAllArgsConstructor() {
        // Given & When
        HashSet<RegulatoryDamEntity> regulatoryDams = new HashSet<>();

        ClassificationDamEntity classification = new ClassificationDamEntity(
                1L,
                "Médio Porte",
                regulatoryDams
        );

        // Then
        assertThat(classification).satisfies(c -> {
            assertThat(c.getId()).isEqualTo(1L);
            assertThat(c.getClassification()).isEqualTo("Médio Porte");
            assertThat(c.getRegulatoryDams()).isEqualTo(regulatoryDams);
        });
    }

    @Test
    @DisplayName("Should create classification dam with no args constructor")
    void shouldCreateClassificationDamWithNoArgsConstructor() {
        // Given & When
        ClassificationDamEntity classification = new ClassificationDamEntity();

        // Then
        assertThat(classification).isNotNull();
        assertThat(classification.getId()).isNull();
        assertThat(classification.getClassification()).isNull();
        assertThat(classification.getRegulatoryDams()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should initialize regulatory dams as empty HashSet")
    void shouldInitializeRegulatoryDamsAsEmptyHashSet() {
        // Given & When
        ClassificationDamEntity classification = new ClassificationDamEntity();

        // Then
        assertThat(classification.getRegulatoryDams())
                .isNotNull()
                .isInstanceOf(HashSet.class)
                .isEmpty();
    }

    @Test
    @DisplayName("Should handle different classification types")
    void shouldHandleDifferentClassificationTypes() {
        // Given
        ClassificationDamEntity pequeno = new ClassificationDamEntity();
        pequeno.setClassification("Pequeno Porte");

        ClassificationDamEntity medio = new ClassificationDamEntity();
        medio.setClassification("Médio Porte");

        ClassificationDamEntity grande = new ClassificationDamEntity();
        grande.setClassification("Grande Porte");

        ClassificationDamEntity especial = new ClassificationDamEntity();
        especial.setClassification("Porte Especial");

        // Then
        assertThat(pequeno.getClassification()).isEqualTo("Pequeno Porte");
        assertThat(medio.getClassification()).isEqualTo("Médio Porte");
        assertThat(grande.getClassification()).isEqualTo("Grande Porte");
        assertThat(especial.getClassification()).isEqualTo("Porte Especial");
    }

    @Test
    @DisplayName("Should enforce unique classification value concept")
    void shouldEnforceUniqueClassificationValueConcept() {
        // Given
        ClassificationDamEntity classification1 = new ClassificationDamEntity();
        classification1.setId(1L);
        classification1.setClassification("Pequeno Porte");

        ClassificationDamEntity classification2 = new ClassificationDamEntity();
        classification2.setId(2L);
        classification2.setClassification("Pequeno Porte");

        // Then - In database, this would violate unique constraint
        // But in entity level, we can validate the names are same
        assertThat(classification1.getClassification()).isEqualTo(classification2.getClassification());
        assertThat(classification1.getId()).isNotEqualTo(classification2.getId());
    }

    @Test
    @DisplayName("Should add regulatory dam to classification")
    void shouldAddRegulatoryDamToClassification() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        classification.setClassification("Grande Porte");

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);
        regulatoryDam.setClassificationDam(classification);

        // When
        classification.getRegulatoryDams().add(regulatoryDam);

        // Then
        assertThat(classification.getRegulatoryDams())
                .hasSize(1)
                .contains(regulatoryDam);
    }

    @Test
    @DisplayName("Should add multiple regulatory dams")
    void shouldAddMultipleRegulatoryDams() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        classification.setClassification("Médio Porte");

        RegulatoryDamEntity dam1 = new RegulatoryDamEntity();
        dam1.setId(1L);
        dam1.setClassificationDam(classification);

        RegulatoryDamEntity dam2 = new RegulatoryDamEntity();
        dam2.setId(2L);
        dam2.setClassificationDam(classification);

        RegulatoryDamEntity dam3 = new RegulatoryDamEntity();
        dam3.setId(3L);
        dam3.setClassificationDam(classification);

        // When
        classification.getRegulatoryDams().add(dam1);
        classification.getRegulatoryDams().add(dam2);
        classification.getRegulatoryDams().add(dam3);

        // Then
        assertThat(classification.getRegulatoryDams())
                .hasSize(3)
                .containsExactlyInAnyOrder(dam1, dam2, dam3);
    }

    @Test
    @DisplayName("Should handle classification name with special characters")
    void shouldHandleClassificationNameWithSpecialCharacters() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        classification.setClassification("Porte Especial - Classe A");

        // Then
        assertThat(classification.getClassification()).contains("-");
    }

    @Test
    @DisplayName("Should handle classification name with accents")
    void shouldHandleClassificationNameWithAccents() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        classification.setClassification("Médio Porte - Hidráulico");

        // Then
        assertThat(classification.getClassification())
                .contains("Médio")
                .contains("Hidráulico");
    }

    @Test
    @DisplayName("Should handle long classification names")
    void shouldHandleLongClassificationNames() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        String longName = "Grande Porte - Barragem de Contenção com Finalidade Múltipla";
        classification.setClassification(longName);

        // Then
        assertThat(classification.getClassification()).isEqualTo(longName);
        assertThat(classification.getClassification().length()).isGreaterThan(30);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        classification.setId(1L);
        classification.setClassification("Original Classification");

        Long originalId = classification.getId();

        // When
        classification.setClassification("Updated Classification");

        // Then
        assertThat(classification.getId()).isEqualTo(originalId);
        assertThat(classification.getClassification()).isEqualTo("Updated Classification");
    }

    @Test
    @DisplayName("Should clear all regulatory dams")
    void shouldClearAllRegulatoryDams() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        RegulatoryDamEntity dam1 = new RegulatoryDamEntity();
        dam1.setId(1L);
        RegulatoryDamEntity dam2 = new RegulatoryDamEntity();
        dam2.setId(2L);

        classification.getRegulatoryDams().add(dam1);
        classification.getRegulatoryDams().add(dam2);

        // When
        classification.getRegulatoryDams().clear();

        // Then
        assertThat(classification.getRegulatoryDams()).isEmpty();
    }

    @Test
    @DisplayName("Should support classification without regulatory dams")
    void shouldSupportClassificationWithoutRegulatoryDams() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        classification.setClassification("Pequeno Porte");

        // Then
        assertThat(classification.getRegulatoryDams()).isEmpty();
        assertThat(classification.getClassification()).isNotBlank();
    }

    @Test
    @DisplayName("Should handle different classification nomenclatures")
    void shouldHandleDifferentClassificationNomenclatures() {
        // Given - Different naming conventions
        ClassificationDamEntity classA = new ClassificationDamEntity();
        classA.setClassification("Classe A");

        ClassificationDamEntity type1 = new ClassificationDamEntity();
        type1.setClassification("Tipo I");

        ClassificationDamEntity category1 = new ClassificationDamEntity();
        category1.setClassification("Categoria 1");

        // Then
        assertThat(classA.getClassification()).contains("Classe");
        assertThat(type1.getClassification()).contains("Tipo");
        assertThat(category1.getClassification()).contains("Categoria");
    }

    @Test
    @DisplayName("Should support alphanumeric classifications")
    void shouldSupportAlphanumericClassifications() {
        // Given
        ClassificationDamEntity classification = new ClassificationDamEntity();
        classification.setClassification("Classe A1 - Porte Grande");

        // Then
        assertThat(classification.getClassification())
                .contains("A1")
                .contains("Grande");
    }
}
