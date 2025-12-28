package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentTabulateAssociationEntity;
import com.geosegbar.entities.InstrumentTabulatePatternEntity;
import com.geosegbar.entities.InstrumentTabulatePatternFolder;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentTabulatePatternEntity")
class InstrumentTabulatePatternEntityTest extends BaseUnitTest {

    private DamEntity dam;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Test");
    }

    @Test
    @DisplayName("Should create tabulate pattern with all required fields")
    void shouldCreateTabulatePatternWithAllRequiredFields() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setId(1L);
        pattern.setName("Padrão de Tabela 1");
        pattern.setDam(dam);

        // Then
        assertThat(pattern).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getName()).isEqualTo("Padrão de Tabela 1");
            assertThat(p.getDam()).isEqualTo(dam);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setId(1L);
        folder.setName("Folder 1");

        // When
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity(
                1L,
                "Padrão de Tabela 1",
                dam,
                folder,
                new HashSet<>()
        );

        // Then
        assertThat(pattern.getId()).isEqualTo(1L);
        assertThat(pattern.getName()).isEqualTo("Padrão de Tabela 1");
        assertThat(pattern.getDam()).isEqualTo(dam);
        assertThat(pattern.getFolder()).isEqualTo(folder);
        assertThat(pattern.getAssociations()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setDam(dam);

        // Then
        assertThat(pattern.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Folder")
    void shouldMaintainManyToOneRelationshipWithFolder() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setId(1L);
        folder.setName("Folder 1");

        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setFolder(folder);

        // Then
        assertThat(pattern.getFolder())
                .isNotNull()
                .isEqualTo(folder);
    }

    @Test
    @DisplayName("Should allow null folder")
    void shouldAllowNullFolder() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setName("Padrão sem pasta");
        pattern.setDam(dam);
        pattern.setFolder(null);

        // Then
        assertThat(pattern.getFolder()).isNull();
        assertThat(pattern.getName()).isNotNull();
        assertThat(pattern.getDam()).isNotNull();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of associations")
    void shouldMaintainOneToManyCollectionOfAssociations() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setAssociations(new HashSet<>());

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(1L);
        pattern.getAssociations().add(association);

        // Then
        assertThat(pattern.getAssociations())
                .isNotNull()
                .hasSize(1)
                .contains(association);
    }

    @Test
    @DisplayName("Should support multiple associations per pattern")
    void shouldSupportMultipleAssociationsPerPattern() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setAssociations(new HashSet<>());

        InstrumentTabulateAssociationEntity assoc1 = new InstrumentTabulateAssociationEntity();
        assoc1.setId(1L);
        InstrumentTabulateAssociationEntity assoc2 = new InstrumentTabulateAssociationEntity();
        assoc2.setId(2L);
        InstrumentTabulateAssociationEntity assoc3 = new InstrumentTabulateAssociationEntity();
        assoc3.setId(3L);

        pattern.getAssociations().add(assoc1);
        pattern.getAssociations().add(assoc2);
        pattern.getAssociations().add(assoc3);

        // Then
        assertThat(pattern.getAssociations()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty associations collection by default")
    void shouldInitializeEmptyAssociationsCollectionByDefault() {
        // Given & When
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();

        // Then
        assertThat(pattern.getAssociations()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing associations")
    void shouldSupportAddingAndRemovingAssociations() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(1L);

        // When
        pattern.getAssociations().add(association);
        assertThat(pattern.getAssociations()).hasSize(1);

        pattern.getAssociations().remove(association);

        // Then
        assertThat(pattern.getAssociations()).isEmpty();
    }

    @Test
    @DisplayName("Should allow multiple patterns per dam")
    void shouldAllowMultiplePatternsPerDam() {
        // Given
        InstrumentTabulatePatternEntity pattern1 = new InstrumentTabulatePatternEntity();
        pattern1.setId(1L);
        pattern1.setName("Padrão 1");
        pattern1.setDam(dam);

        InstrumentTabulatePatternEntity pattern2 = new InstrumentTabulatePatternEntity();
        pattern2.setId(2L);
        pattern2.setName("Padrão 2");
        pattern2.setDam(dam);

        // Then
        assertThat(pattern1.getDam()).isEqualTo(pattern2.getDam());
        assertThat(pattern1.getId()).isNotEqualTo(pattern2.getId());
        assertThat(pattern1.getName()).isNotEqualTo(pattern2.getName());
    }

    @Test
    @DisplayName("Should allow multiple patterns per folder")
    void shouldAllowMultiplePatternsPerFolder() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setId(1L);
        folder.setName("Folder 1");

        InstrumentTabulatePatternEntity pattern1 = new InstrumentTabulatePatternEntity();
        pattern1.setId(1L);
        pattern1.setName("Padrão A");
        pattern1.setFolder(folder);

        InstrumentTabulatePatternEntity pattern2 = new InstrumentTabulatePatternEntity();
        pattern2.setId(2L);
        pattern2.setName("Padrão B");
        pattern2.setFolder(folder);

        // Then
        assertThat(pattern1.getFolder()).isEqualTo(pattern2.getFolder());
        assertThat(pattern1.getId()).isNotEqualTo(pattern2.getId());
    }

    @Test
    @DisplayName("Should support descriptive pattern names")
    void shouldSupportDescriptivePatternNames() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setName("Padrão de Tabela Completo com Todas as Leituras e Dados Hidrológicos");

        // Then
        assertThat(pattern.getName()).hasSize(68);
    }

    @Test
    @DisplayName("Should support short pattern names")
    void shouldSupportShortPatternNames() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setName("Padrão1");

        // Then
        assertThat(pattern.getName()).hasSize(7);
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setName("Padrão de Monitoração Hidrológica");

        // Then
        assertThat(pattern.getName()).contains("ã", "ó");
    }

    @Test
    @DisplayName("Should support special characters in name")
    void shouldSupportSpecialCharactersInName() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setName("Padrão Básico (Instrumentação) - 2024");

        // Then
        assertThat(pattern.getName()).contains("(", ")", "-");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setId(1L);
        pattern.setName("Nome Inicial");

        Long originalId = pattern.getId();

        // When
        pattern.setName("Nome Atualizado");

        // Then
        assertThat(pattern.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support pattern organization by dam")
    void shouldSupportPatternOrganizationByDam() {
        // Given
        DamEntity dam2 = new DamEntity();
        dam2.setId(2L);
        dam2.setName("Barragem 2");

        InstrumentTabulatePatternEntity patternDam1 = new InstrumentTabulatePatternEntity();
        patternDam1.setDam(dam);

        InstrumentTabulatePatternEntity patternDam2 = new InstrumentTabulatePatternEntity();
        patternDam2.setDam(dam2);

        // Then
        assertThat(patternDam1.getDam()).isNotEqualTo(patternDam2.getDam());
    }

    @Test
    @DisplayName("Should support cascade operations on associations")
    void shouldSupportCascadeOperationsOnAssociations() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(1L);
        association.setPattern(pattern);

        // When
        pattern.getAssociations().add(association);

        // Then - Association references pattern
        assertThat(association.getPattern()).isEqualTo(pattern);
        assertThat(pattern.getAssociations()).contains(association);
    }

    @Test
    @DisplayName("Should support orphan removal for associations")
    void shouldSupportOrphanRemovalForAssociations() {
        // Given
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(1L);
        pattern.getAssociations().add(association);

        // When - Remove from collection
        pattern.getAssociations().remove(association);

        // Then - Association removed from collection
        assertThat(pattern.getAssociations()).doesNotContain(association);
    }

    @Test
    @DisplayName("Should support patterns without folder organization")
    void shouldSupportPatternsWithoutFolderOrganization() {
        // Given
        InstrumentTabulatePatternEntity pattern1 = new InstrumentTabulatePatternEntity();
        pattern1.setName("Padrão Global 1");
        pattern1.setDam(dam);
        pattern1.setFolder(null);

        InstrumentTabulatePatternEntity pattern2 = new InstrumentTabulatePatternEntity();
        pattern2.setName("Padrão Global 2");
        pattern2.setDam(dam);
        pattern2.setFolder(null);

        // Then - Both patterns without folder
        assertThat(pattern1.getFolder()).isNull();
        assertThat(pattern2.getFolder()).isNull();
        assertThat(pattern1.getDam()).isEqualTo(pattern2.getDam());
    }

    @Test
    @DisplayName("Should support complete pattern configuration")
    void shouldSupportCompletePatternConfiguration() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setId(1L);
        folder.setName("Folder Completo");

        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setId(1L);
        pattern.setName("Padrão Completo com Todas as Configurações");
        pattern.setDam(dam);
        pattern.setFolder(folder);

        InstrumentTabulateAssociationEntity assoc1 = new InstrumentTabulateAssociationEntity();
        assoc1.setId(1L);
        InstrumentTabulateAssociationEntity assoc2 = new InstrumentTabulateAssociationEntity();
        assoc2.setId(2L);

        pattern.getAssociations().add(assoc1);
        pattern.getAssociations().add(assoc2);

        // Then
        assertThat(pattern.getId()).isNotNull();
        assertThat(pattern.getName()).isNotBlank();
        assertThat(pattern.getDam()).isNotNull();
        assertThat(pattern.getFolder()).isNotNull();
        assertThat(pattern.getAssociations()).hasSize(2);
    }
}
