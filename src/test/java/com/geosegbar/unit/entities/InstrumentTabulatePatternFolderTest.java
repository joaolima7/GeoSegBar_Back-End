package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentTabulatePatternEntity;
import com.geosegbar.entities.InstrumentTabulatePatternFolder;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentTabulatePatternFolder")
class InstrumentTabulatePatternFolderTest extends BaseUnitTest {

    private DamEntity dam;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Test");
    }

    @Test
    @DisplayName("Should create tabulate pattern folder with all required fields")
    void shouldCreateTabulatePatternFolderWithAllRequiredFields() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setId(1L);
        folder.setName("Pasta de Padrões");
        folder.setDam(dam);

        // Then
        assertThat(folder).satisfies(f -> {
            assertThat(f.getId()).isEqualTo(1L);
            assertThat(f.getName()).isEqualTo("Pasta de Padrões");
            assertThat(f.getDam()).isEqualTo(dam);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder(
                1L,
                "Pasta de Padrões",
                dam,
                new HashSet<>()
        );

        // Then
        assertThat(folder.getId()).isEqualTo(1L);
        assertThat(folder.getName()).isEqualTo("Pasta de Padrões");
        assertThat(folder.getDam()).isEqualTo(dam);
        assertThat(folder.getPatterns()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setDam(dam);

        // Then
        assertThat(folder.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of patterns")
    void shouldMaintainOneToManyCollectionOfPatterns() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setPatterns(new HashSet<>());

        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setId(1L);
        pattern.setName("Padrão 1");
        folder.getPatterns().add(pattern);

        // Then
        assertThat(folder.getPatterns())
                .isNotNull()
                .hasSize(1)
                .contains(pattern);
    }

    @Test
    @DisplayName("Should support multiple patterns per folder")
    void shouldSupportMultiplePatternsPerFolder() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setPatterns(new HashSet<>());

        InstrumentTabulatePatternEntity pattern1 = new InstrumentTabulatePatternEntity();
        pattern1.setId(1L);
        pattern1.setName("Padrão 1");

        InstrumentTabulatePatternEntity pattern2 = new InstrumentTabulatePatternEntity();
        pattern2.setId(2L);
        pattern2.setName("Padrão 2");

        InstrumentTabulatePatternEntity pattern3 = new InstrumentTabulatePatternEntity();
        pattern3.setId(3L);
        pattern3.setName("Padrão 3");

        folder.getPatterns().add(pattern1);
        folder.getPatterns().add(pattern2);
        folder.getPatterns().add(pattern3);

        // Then
        assertThat(folder.getPatterns()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty patterns collection by default")
    void shouldInitializeEmptyPatternsCollectionByDefault() {
        // Given & When
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();

        // Then
        assertThat(folder.getPatterns()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing patterns")
    void shouldSupportAddingAndRemovingPatterns() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setId(1L);
        pattern.setName("Padrão Test");

        // When
        folder.getPatterns().add(pattern);
        assertThat(folder.getPatterns()).hasSize(1);

        folder.getPatterns().remove(pattern);

        // Then
        assertThat(folder.getPatterns()).isEmpty();
    }

    @Test
    @DisplayName("Should allow multiple folders per dam")
    void shouldAllowMultipleFoldersPerDam() {
        // Given
        InstrumentTabulatePatternFolder folder1 = new InstrumentTabulatePatternFolder();
        folder1.setId(1L);
        folder1.setName("Pasta 1");
        folder1.setDam(dam);

        InstrumentTabulatePatternFolder folder2 = new InstrumentTabulatePatternFolder();
        folder2.setId(2L);
        folder2.setName("Pasta 2");
        folder2.setDam(dam);

        // Then
        assertThat(folder1.getDam()).isEqualTo(folder2.getDam());
        assertThat(folder1.getId()).isNotEqualTo(folder2.getId());
        assertThat(folder1.getName()).isNotEqualTo(folder2.getName());
    }

    @Test
    @DisplayName("Should support descriptive folder names")
    void shouldSupportDescriptiveFolderNames() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setName("Pasta de Padrões de Tabulação para Instrumentação Hidráulica");

        // Then
        assertThat(folder.getName()).hasSize(60);
    }

    @Test
    @DisplayName("Should support short folder names")
    void shouldSupportShortFolderNames() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setName("Padrões");

        // Then
        assertThat(folder.getName()).hasSize(7);
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setName("Padrões de Instrumentação");

        // Then
        assertThat(folder.getName()).contains("õ", "ã");
    }

    @Test
    @DisplayName("Should support special characters in name")
    void shouldSupportSpecialCharactersInName() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setName("Pasta (Principal) - 2024");

        // Then
        assertThat(folder.getName()).contains("(", ")", "-");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setId(1L);
        folder.setName("Nome Inicial");

        Long originalId = folder.getId();

        // When
        folder.setName("Nome Atualizado");

        // Then
        assertThat(folder.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support folder organization by dam")
    void shouldSupportFolderOrganizationByDam() {
        // Given
        DamEntity dam2 = new DamEntity();
        dam2.setId(2L);
        dam2.setName("Barragem 2");

        InstrumentTabulatePatternFolder folderDam1 = new InstrumentTabulatePatternFolder();
        folderDam1.setDam(dam);

        InstrumentTabulatePatternFolder folderDam2 = new InstrumentTabulatePatternFolder();
        folderDam2.setDam(dam2);

        // Then
        assertThat(folderDam1.getDam()).isNotEqualTo(folderDam2.getDam());
    }

    @Test
    @DisplayName("Should support empty folders")
    void shouldSupportEmptyFolders() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setName("Pasta Vazia");
        folder.setDam(dam);

        // Then
        assertThat(folder.getPatterns()).isEmpty();
        assertThat(folder.getName()).isNotBlank();
        assertThat(folder.getDam()).isNotNull();
    }

    @Test
    @DisplayName("Should support hierarchical naming")
    void shouldSupportHierarchicalNaming() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setName("Instrumentação/Hidráulica/Padrões");

        // Then
        assertThat(folder.getName()).contains("/");
    }

    @Test
    @DisplayName("Should support lazy fetch for patterns collection")
    void shouldSupportLazyFetchForPatternsCollection() {
        // Given
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setName("Pasta Lazy");
        folder.setDam(dam);

        // Then - Patterns collection initialized but lazy
        assertThat(folder.getPatterns()).isNotNull();
    }
}
