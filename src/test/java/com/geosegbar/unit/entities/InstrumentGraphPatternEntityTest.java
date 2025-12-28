package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphAxesEntity;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.InstrumentGraphPatternFolder;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentGraphPatternEntity")
class InstrumentGraphPatternEntityTest extends BaseUnitTest {

    private InstrumentEntity instrument;
    private InstrumentGraphPatternFolder folder;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("Instrumento 1");

        folder = new InstrumentGraphPatternFolder();
        folder.setId(1L);
        folder.setName("Pasta 1");
    }

    @Test
    @DisplayName("Should create graph pattern with all required fields")
    void shouldCreateGraphPatternWithAllRequiredFields() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setId(1L);
        pattern.setName("Pattern 1");
        pattern.setInstrument(instrument);

        // Then
        assertThat(pattern).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getName()).isEqualTo("Pattern 1");
            assertThat(p.getInstrument()).isEqualTo(instrument);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity(
                1L,
                "Pattern 1",
                instrument,
                folder,
                new HashSet<>(),
                null
        );

        // Then
        assertThat(pattern.getId()).isEqualTo(1L);
        assertThat(pattern.getName()).isEqualTo("Pattern 1");
        assertThat(pattern.getInstrument()).isEqualTo(instrument);
        assertThat(pattern.getFolder()).isEqualTo(folder);
        assertThat(pattern.getProperties()).isNotNull().isEmpty();
        assertThat(pattern.getAxes()).isNull();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Instrument")
    void shouldMaintainManyToOneRelationshipWithInstrument() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setInstrument(instrument);

        // Then
        assertThat(pattern.getInstrument())
                .isNotNull()
                .isEqualTo(instrument);
    }

    @Test
    @DisplayName("Should maintain optional ManyToOne relationship with Folder")
    void shouldMaintainOptionalManyToOneRelationshipWithFolder() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setFolder(folder);

        // Then
        assertThat(pattern.getFolder())
                .isNotNull()
                .isEqualTo(folder);
    }

    @Test
    @DisplayName("Should allow pattern without folder")
    void shouldAllowPatternWithoutFolder() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setFolder(null);

        // Then
        assertThat(pattern.getFolder()).isNull();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of properties")
    void shouldMaintainOneToManyCollectionOfProperties() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setProperties(new HashSet<>());

        InstrumentGraphCustomizationPropertiesEntity property = new InstrumentGraphCustomizationPropertiesEntity();
        property.setId(1L);
        pattern.getProperties().add(property);

        // Then
        assertThat(pattern.getProperties())
                .isNotNull()
                .hasSize(1)
                .contains(property);
    }

    @Test
    @DisplayName("Should support multiple customization properties")
    void shouldSupportMultipleCustomizationProperties() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setProperties(new HashSet<>());

        InstrumentGraphCustomizationPropertiesEntity property1 = new InstrumentGraphCustomizationPropertiesEntity();
        property1.setId(1L);
        InstrumentGraphCustomizationPropertiesEntity property2 = new InstrumentGraphCustomizationPropertiesEntity();
        property2.setId(2L);

        pattern.getProperties().add(property1);
        pattern.getProperties().add(property2);

        // Then
        assertThat(pattern.getProperties()).hasSize(2);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with axes")
    void shouldMaintainOneToOneRelationshipWithAxes() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setId(1L);
        pattern.setAxes(axes);

        // Then
        assertThat(pattern.getAxes())
                .isNotNull()
                .isEqualTo(axes);
    }

    @Test
    @DisplayName("Should allow pattern without axes")
    void shouldAllowPatternWithoutAxes() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setAxes(null);

        // Then
        assertThat(pattern.getAxes()).isNull();
    }

    @Test
    @DisplayName("Should initialize empty properties collection by default")
    void shouldInitializeEmptyPropertiesCollectionByDefault() {
        // Given & When
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();

        // Then
        assertThat(pattern.getProperties()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing properties")
    void shouldSupportAddingAndRemovingProperties() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        InstrumentGraphCustomizationPropertiesEntity property = new InstrumentGraphCustomizationPropertiesEntity();
        property.setId(1L);

        // When
        pattern.getProperties().add(property);
        assertThat(pattern.getProperties()).hasSize(1);

        pattern.getProperties().remove(property);

        // Then
        assertThat(pattern.getProperties()).isEmpty();
    }

    @Test
    @DisplayName("Should allow multiple patterns per instrument")
    void shouldAllowMultiplePatternsPerInstrument() {
        // Given
        InstrumentGraphPatternEntity pattern1 = new InstrumentGraphPatternEntity();
        pattern1.setId(1L);
        pattern1.setName("Pattern 1");
        pattern1.setInstrument(instrument);

        InstrumentGraphPatternEntity pattern2 = new InstrumentGraphPatternEntity();
        pattern2.setId(2L);
        pattern2.setName("Pattern 2");
        pattern2.setInstrument(instrument);

        // Then
        assertThat(pattern1.getInstrument()).isEqualTo(pattern2.getInstrument());
        assertThat(pattern1.getId()).isNotEqualTo(pattern2.getId());
        assertThat(pattern1.getName()).isNotEqualTo(pattern2.getName());
    }

    @Test
    @DisplayName("Should allow multiple patterns per folder")
    void shouldAllowMultiplePatternsPerFolder() {
        // Given
        InstrumentGraphPatternEntity pattern1 = new InstrumentGraphPatternEntity();
        pattern1.setId(1L);
        pattern1.setFolder(folder);

        InstrumentGraphPatternEntity pattern2 = new InstrumentGraphPatternEntity();
        pattern2.setId(2L);
        pattern2.setFolder(folder);

        // Then
        assertThat(pattern1.getFolder()).isEqualTo(pattern2.getFolder());
        assertThat(pattern1.getId()).isNotEqualTo(pattern2.getId());
    }

    @Test
    @DisplayName("Should support descriptive pattern names")
    void shouldSupportDescriptivePatternNames() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setName("Gráfico de Pressão - Visualização Mensal");

        // Then
        assertThat(pattern.getName()).isEqualTo("Gráfico de Pressão - Visualização Mensal");
    }

    @Test
    @DisplayName("Should support short pattern names")
    void shouldSupportShortPatternNames() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setName("P1");

        // Then
        assertThat(pattern.getName()).hasSize(2);
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setName("Padrão de Visualização");

        // Then
        assertThat(pattern.getName()).contains("ã", "ç");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setId(1L);
        pattern.setName("Pattern 1");

        Long originalId = pattern.getId();

        // When
        pattern.setName("Pattern 1 Modified");
        pattern.setFolder(folder);

        // Then
        assertThat(pattern.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support cascade operations on properties collection")
    void shouldSupportCascadeOperationsOnPropertiesCollection() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        InstrumentGraphCustomizationPropertiesEntity property = new InstrumentGraphCustomizationPropertiesEntity();
        property.setId(1L);
        property.setPattern(pattern);

        // When
        pattern.getProperties().add(property);

        // Then - Property references pattern
        assertThat(property.getPattern()).isEqualTo(pattern);
        assertThat(pattern.getProperties()).contains(property);
    }

    @Test
    @DisplayName("Should support orphan removal for properties")
    void shouldSupportOrphanRemovalForProperties() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        InstrumentGraphCustomizationPropertiesEntity property = new InstrumentGraphCustomizationPropertiesEntity();
        property.setId(1L);
        pattern.getProperties().add(property);

        // When - Remove from collection
        pattern.getProperties().remove(property);

        // Then - Property removed from collection
        assertThat(pattern.getProperties()).doesNotContain(property);
    }

    @Test
    @DisplayName("Should support cascade operations on axes relationship")
    void shouldSupportCascadeOperationsOnAxesRelationship() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setId(1L);
        axes.setPattern(pattern);

        // When
        pattern.setAxes(axes);

        // Then - Axes references pattern
        assertThat(axes.getPattern()).isEqualTo(pattern);
        assertThat(pattern.getAxes()).isEqualTo(axes);
    }

    @Test
    @DisplayName("Should support replacing axes")
    void shouldSupportReplacingAxes() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        InstrumentGraphAxesEntity axes1 = new InstrumentGraphAxesEntity();
        axes1.setId(1L);
        pattern.setAxes(axes1);

        InstrumentGraphAxesEntity axes2 = new InstrumentGraphAxesEntity();
        axes2.setId(2L);

        // When
        pattern.setAxes(axes2);

        // Then
        assertThat(pattern.getAxes()).isEqualTo(axes2);
    }

    @Test
    @DisplayName("Should support complete pattern configuration")
    void shouldSupportCompletePatternConfiguration() {
        // Given
        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setName("Complete Pattern");
        pattern.setInstrument(instrument);
        pattern.setFolder(folder);

        InstrumentGraphCustomizationPropertiesEntity property = new InstrumentGraphCustomizationPropertiesEntity();
        property.setId(1L);
        pattern.getProperties().add(property);

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setId(1L);
        pattern.setAxes(axes);

        // Then
        assertThat(pattern.getName()).isNotBlank();
        assertThat(pattern.getInstrument()).isNotNull();
        assertThat(pattern.getFolder()).isNotNull();
        assertThat(pattern.getProperties()).isNotEmpty();
        assertThat(pattern.getAxes()).isNotNull();
    }
}
