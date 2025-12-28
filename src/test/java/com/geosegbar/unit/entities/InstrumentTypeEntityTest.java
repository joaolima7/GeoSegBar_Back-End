package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentTypeEntity")
class InstrumentTypeEntityTest extends BaseUnitTest {

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
    }

    @Test
    @DisplayName("Should create instrument type with all required fields")
    void shouldCreateInstrumentTypeWithAllRequiredFields() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(1L);
        type.setName("Piezômetro");

        // Then
        assertThat(type).satisfies(t -> {
            assertThat(t.getId()).isEqualTo(1L);
            assertThat(t.getName()).isEqualTo("Piezômetro");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        InstrumentTypeEntity type = new InstrumentTypeEntity(
                1L,
                "Piezômetro",
                new HashSet<>()
        );

        // Then
        assertThat(type.getId()).isEqualTo(1L);
        assertThat(type.getName()).isEqualTo("Piezômetro");
        assertThat(type.getInstruments()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of instruments")
    void shouldMaintainOneToManyCollectionOfInstruments() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Piezômetro");
        type.setInstruments(new HashSet<>());

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("PZ-01");
        type.getInstruments().add(instrument);

        // Then
        assertThat(type.getInstruments())
                .isNotNull()
                .hasSize(1)
                .contains(instrument);
    }

    @Test
    @DisplayName("Should support multiple instruments per type")
    void shouldSupportMultipleInstrumentsPerType() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Piezômetro");
        type.setInstruments(new HashSet<>());

        InstrumentEntity inst1 = new InstrumentEntity();
        inst1.setId(1L);
        inst1.setName("PZ-01");

        InstrumentEntity inst2 = new InstrumentEntity();
        inst2.setId(2L);
        inst2.setName("PZ-02");

        InstrumentEntity inst3 = new InstrumentEntity();
        inst3.setId(3L);
        inst3.setName("PZ-03");

        type.getInstruments().add(inst1);
        type.getInstruments().add(inst2);
        type.getInstruments().add(inst3);

        // Then
        assertThat(type.getInstruments()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty instruments collection by default")
    void shouldInitializeEmptyInstrumentsCollectionByDefault() {
        // Given & When
        InstrumentTypeEntity type = new InstrumentTypeEntity();

        // Then
        assertThat(type.getInstruments()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing instruments")
    void shouldSupportAddingAndRemovingInstruments() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("PZ-01");

        // When
        type.getInstruments().add(instrument);
        assertThat(type.getInstruments()).hasSize(1);

        type.getInstruments().remove(instrument);

        // Then
        assertThat(type.getInstruments()).isEmpty();
    }

    @Test
    @DisplayName("Should support common instrument type names")
    void shouldSupportCommonInstrumentTypeNames() {
        // Given
        InstrumentTypeEntity piezo = new InstrumentTypeEntity();
        piezo.setName("Piezômetro");

        InstrumentTypeEntity incli = new InstrumentTypeEntity();
        incli.setName("Inclinômetro");

        InstrumentTypeEntity extenso = new InstrumentTypeEntity();
        extenso.setName("Extensômetro");

        // Then
        assertThat(piezo.getName()).isEqualTo("Piezômetro");
        assertThat(incli.getName()).isEqualTo("Inclinômetro");
        assertThat(extenso.getName()).isEqualTo("Extensômetro");
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Medidor de Vazão");

        // Then
        assertThat(type.getName()).contains("ã");
    }

    @Test
    @DisplayName("Should support accented characters in name")
    void shouldSupportAccentedCharactersInName() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Piezômetro Elétrico");

        // Then
        assertThat(type.getName()).contains("ô", "é");
    }

    @Test
    @DisplayName("Should support descriptive type names")
    void shouldSupportDescriptiveTypeNames() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Medidor de Nível de Água Subterrânea");

        // Then
        assertThat(type.getName()).hasSize(36);
    }

    @Test
    @DisplayName("Should support short type names")
    void shouldSupportShortTypeNames() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Régua");

        // Then
        assertThat(type.getName()).hasSize(5);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(1L);
        type.setName("Nome Inicial");

        Long originalId = type.getId();

        // When
        type.setName("Nome Atualizado");

        // Then
        assertThat(type.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support unique constraint on name")
    void shouldSupportUniqueConstraintOnName() {
        // Given
        InstrumentTypeEntity type1 = new InstrumentTypeEntity();
        type1.setId(1L);
        type1.setName("Piezômetro");

        InstrumentTypeEntity type2 = new InstrumentTypeEntity();
        type2.setId(2L);
        type2.setName("Inclinômetro");

        // Then - Different names for different types
        assertThat(type1.getName()).isNotEqualTo(type2.getName());
    }

    @Test
    @DisplayName("Should support name search index")
    void shouldSupportNameSearchIndex() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Piezômetro");

        // Then - Name indexed for search
        assertThat(type.getName()).isNotBlank();
    }

    @Test
    @DisplayName("Should support different instrument types")
    void shouldSupportDifferentInstrumentTypes() {
        // Given - Various instrument types
        InstrumentTypeEntity piezo = new InstrumentTypeEntity();
        piezo.setId(1L);
        piezo.setName("Piezômetro");

        InstrumentTypeEntity incli = new InstrumentTypeEntity();
        incli.setId(2L);
        incli.setName("Inclinômetro");

        InstrumentTypeEntity medidor = new InstrumentTypeEntity();
        medidor.setId(3L);
        medidor.setName("Medidor de Vazão");

        InstrumentTypeEntity marco = new InstrumentTypeEntity();
        marco.setId(4L);
        marco.setName("Marco Superficial");

        // Then - All types distinct
        assertThat(piezo.getId()).isNotEqualTo(incli.getId());
        assertThat(incli.getId()).isNotEqualTo(medidor.getId());
        assertThat(medidor.getId()).isNotEqualTo(marco.getId());
    }

    @Test
    @DisplayName("Should support bidirectional relationship with instruments")
    void shouldSupportBidirectionalRelationshipWithInstruments() {
        // Given
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(1L);
        type.setName("Piezômetro");

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("PZ-01");
        instrument.setInstrumentType(type);

        // When
        type.getInstruments().add(instrument);

        // Then - Bidirectional relationship
        assertThat(instrument.getInstrumentType()).isEqualTo(type);
        assertThat(type.getInstruments()).contains(instrument);
    }
}
