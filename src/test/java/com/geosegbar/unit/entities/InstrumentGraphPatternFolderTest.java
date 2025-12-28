package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.InstrumentGraphPatternFolder;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentGraphPatternFolder")
class InstrumentGraphPatternFolderTest extends BaseUnitTest {

    private DamEntity dam;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        dam = TestDataBuilder.dam().build();
    }

    @Test
    @DisplayName("Should create pattern folder with all required fields")
    void shouldCreatePatternFolderWithAllRequiredFields() {
        // Given
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setId(1L);
        folder.setName("Folder 1");
        folder.setDam(dam);

        // Then
        assertThat(folder).satisfies(f -> {
            assertThat(f.getId()).isEqualTo(1L);
            assertThat(f.getName()).isEqualTo("Folder 1");
            assertThat(f.getDam()).isEqualTo(dam);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder(
                1L,
                "Folder 1",
                dam,
                new HashSet<>()
        );

        // Then
        assertThat(folder.getId()).isEqualTo(1L);
        assertThat(folder.getName()).isEqualTo("Folder 1");
        assertThat(folder.getDam()).isEqualTo(dam);
        assertThat(folder.getPatterns()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {
        // Given
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
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
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setPatterns(new HashSet<>());

        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setId(1L);
        pattern.setName("Pattern 1");
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
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setPatterns(new HashSet<>());

        InstrumentGraphPatternEntity pattern1 = new InstrumentGraphPatternEntity();
        pattern1.setId(1L);
        pattern1.setName("Pattern 1");

        InstrumentGraphPatternEntity pattern2 = new InstrumentGraphPatternEntity();
        pattern2.setId(2L);
        pattern2.setName("Pattern 2");

        folder.getPatterns().add(pattern1);
        folder.getPatterns().add(pattern2);

        // Then
        assertThat(folder.getPatterns()).hasSize(2);
    }

    @Test
    @DisplayName("Should initialize empty patterns collection by default")
    void shouldInitializeEmptyPatternsCollectionByDefault() {
        // Given & When
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();

        // Then
        assertThat(folder.getPatterns()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing patterns")
    void shouldSupportAddingAndRemovingPatterns() {
        // Given
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setId(1L);
        pattern.setName("Pattern 1");

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
        InstrumentGraphPatternFolder folder1 = new InstrumentGraphPatternFolder();
        folder1.setId(1L);
        folder1.setName("Folder 1");
        folder1.setDam(dam);

        InstrumentGraphPatternFolder folder2 = new InstrumentGraphPatternFolder();
        folder2.setId(2L);
        folder2.setName("Folder 2");
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
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setName("Padrões de Visualização - Piezômetros");

        // Then
        assertThat(folder.getName()).isEqualTo("Padrões de Visualização - Piezômetros");
    }

    @Test
    @DisplayName("Should support short folder names")
    void shouldSupportShortFolderNames() {
        // Given
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setName("Graficos");

        // Then
        assertThat(folder.getName()).hasSize(8);
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {
        // Given
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setName("Visualizações Padrão");

        // Then
        assertThat(folder.getName()).contains("õ", "ã");
    }

    @Test
    @DisplayName("Should support special characters in name")
    void shouldSupportSpecialCharactersInName() {
        // Given
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setName("Gráficos (Seção 1) - 2024");

        // Then
        assertThat(folder.getName()).contains("(", ")", "-");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setId(1L);
        folder.setName("Folder 1");

        Long originalId = folder.getId();

        // When
        folder.setName("Folder 1 Modified");

        // Then
        assertThat(folder.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support folder organization by dam")
    void shouldSupportFolderOrganizationByDam() {
        // Given
        DamEntity dam2 = TestDataBuilder.dam().withName("Dam 2").build();

        InstrumentGraphPatternFolder folder1 = new InstrumentGraphPatternFolder();
        folder1.setDam(dam);
        folder1.setName("Folder Dam 1");

        InstrumentGraphPatternFolder folder2 = new InstrumentGraphPatternFolder();
        folder2.setDam(dam2);
        folder2.setName("Folder Dam 2");

        // Then
        assertThat(folder1.getDam()).isNotEqualTo(folder2.getDam());
    }

    @Test
    @DisplayName("Should support empty folders")
    void shouldSupportEmptyFolders() {
        // Given
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setName("Empty Folder");
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
        InstrumentGraphPatternFolder parentFolder = new InstrumentGraphPatternFolder();
        parentFolder.setName("Visualizações");

        InstrumentGraphPatternFolder childFolder = new InstrumentGraphPatternFolder();
        childFolder.setName("Visualizações / Piezômetros");

        // Then
        assertThat(childFolder.getName()).contains("/");
    }

    @Test
    @DisplayName("Should support lazy fetch for patterns")
    void shouldSupportLazyFetchForPatterns() {
        // Given
        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setDam(dam);
        folder.setName("Folder 1");

        // Then - Patterns collection initialized but empty (lazy)
        assertThat(folder.getPatterns()).isNotNull();
    }
}
