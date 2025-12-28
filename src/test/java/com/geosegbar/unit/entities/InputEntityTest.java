package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.InputEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InputEntity")
class InputEntityTest extends BaseUnitTest {

    private MeasurementUnitEntity measurementUnit;
    private InstrumentEntity instrument;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        measurementUnit = new MeasurementUnitEntity();
        measurementUnit.setId(1L);
        measurementUnit.setName("Metros");
        measurementUnit.setAcronym("m");

        // Create InstrumentEntity directly
        instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("Instrumento 1");
        instrument.setLatitude(-23.5505);
        instrument.setLongitude(-46.6333);
        instrument.setNoLimit(false);
        instrument.setActive(true);
    }

    @Test
    @DisplayName("Should create input with all required fields")
    void shouldCreateInputWithAllRequiredFields() {
        // Given
        InputEntity input = new InputEntity();
        input.setId(1L);
        input.setAcronym("IN01");
        input.setName("Entrada 01");
        input.setPrecision(2);
        input.setMeasurementUnit(measurementUnit);
        input.setInstrument(instrument);

        // Then
        assertThat(input).satisfies(i -> {
            assertThat(i.getId()).isEqualTo(1L);
            assertThat(i.getAcronym()).isEqualTo("IN01");
            assertThat(i.getName()).isEqualTo("Entrada 01");
            assertThat(i.getPrecision()).isEqualTo(2);
            assertThat(i.getMeasurementUnit()).isEqualTo(measurementUnit);
            assertThat(i.getInstrument()).isEqualTo(instrument);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        InputEntity input = new InputEntity(
                1L,
                "IN01",
                "Entrada 01",
                2,
                measurementUnit,
                instrument
        );

        // Then
        assertThat(input.getId()).isEqualTo(1L);
        assertThat(input.getAcronym()).isEqualTo("IN01");
        assertThat(input.getName()).isEqualTo("Entrada 01");
        assertThat(input.getPrecision()).isEqualTo(2);
        assertThat(input.getMeasurementUnit()).isEqualTo(measurementUnit);
        assertThat(input.getInstrument()).isEqualTo(instrument);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with MeasurementUnit")
    void shouldMaintainManyToOneRelationshipWithMeasurementUnit() {
        // Given
        InputEntity input = new InputEntity();
        input.setMeasurementUnit(measurementUnit);

        // Then
        assertThat(input.getMeasurementUnit())
                .isNotNull()
                .isEqualTo(measurementUnit);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Instrument")
    void shouldMaintainManyToOneRelationshipWithInstrument() {
        // Given
        InputEntity input = new InputEntity();
        input.setInstrument(instrument);

        // Then
        assertThat(input.getInstrument())
                .isNotNull()
                .isEqualTo(instrument);
    }

    @Test
    @DisplayName("Should support zero precision")
    void shouldSupportZeroPrecision() {
        // Given
        InputEntity input = new InputEntity();
        input.setPrecision(0);

        // Then
        assertThat(input.getPrecision()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should support high precision values")
    void shouldSupportHighPrecisionValues() {
        // Given
        InputEntity input = new InputEntity();
        input.setPrecision(10);

        // Then
        assertThat(input.getPrecision()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should support single character acronym")
    void shouldSupportSingleCharacterAcronym() {
        // Given
        InputEntity input = new InputEntity();
        input.setAcronym("I");

        // Then
        assertThat(input.getAcronym())
                .isNotBlank()
                .hasSize(1)
                .isEqualTo("I");
    }

    @Test
    @DisplayName("Should support multiple character acronym")
    void shouldSupportMultipleCharacterAcronym() {
        // Given
        InputEntity input = new InputEntity();
        input.setAcronym("INPUT_01");

        // Then
        assertThat(input.getAcronym())
                .isNotBlank()
                .contains("INPUT");
    }

    @Test
    @DisplayName("Should support Greek letters in acronym")
    void shouldSupportGreekLettersInAcronym() {
        // Given
        InputEntity input = new InputEntity();
        input.setAcronym("Δh");

        // Then
        assertThat(input.getAcronym()).isEqualTo("Δh");
    }

    @Test
    @DisplayName("Should support numeric acronyms")
    void shouldSupportNumericAcronyms() {
        // Given
        InputEntity input = new InputEntity();
        input.setAcronym("01");

        // Then
        assertThat(input.getAcronym()).isEqualTo("01");
    }

    @Test
    @DisplayName("Should support underscores in acronym")
    void shouldSupportUnderscoresInAcronym() {
        // Given
        InputEntity input = new InputEntity();
        input.setAcronym("IN_01");

        // Then
        assertThat(input.getAcronym()).contains("_");
    }

    @Test
    @DisplayName("Should support long names")
    void shouldSupportLongNames() {
        // Given
        String longName = "Entrada de Dados do Sistema de Monitoramento Hidráulico";
        InputEntity input = new InputEntity();
        input.setName(longName);

        // Then
        assertThat(input.getName()).isEqualTo(longName);
    }

    @Test
    @DisplayName("Should support special characters in name")
    void shouldSupportSpecialCharactersInName() {
        // Given
        InputEntity input = new InputEntity();
        input.setName("Entrada (Teste) - Medição #1");

        // Then
        assertThat(input.getName()).contains("(", ")", "-", "#");
    }

    @Test
    @DisplayName("Should allow multiple inputs per instrument")
    void shouldAllowMultipleInputsPerInstrument() {
        // Given
        InputEntity input1 = new InputEntity();
        input1.setId(1L);
        input1.setAcronym("IN01");
        input1.setInstrument(instrument);

        InputEntity input2 = new InputEntity();
        input2.setId(2L);
        input2.setAcronym("IN02");
        input2.setInstrument(instrument);

        // Then
        assertThat(input1.getInstrument()).isEqualTo(input2.getInstrument());
        assertThat(input1.getId()).isNotEqualTo(input2.getId());
        assertThat(input1.getAcronym()).isNotEqualTo(input2.getAcronym());
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InputEntity input = new InputEntity();
        input.setId(1L);
        input.setAcronym("IN01");

        Long originalId = input.getId();

        // When
        input.setAcronym("IN02");
        input.setName("Entrada Modificada");

        // Then
        assertThat(input.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should differentiate inputs from constants")
    void shouldDifferentiateInputsFromConstants() {
        // Given
        InputEntity input = new InputEntity();
        input.setAcronym("IN");
        input.setName("Input Variável");

        // Then - Inputs are variable data inputs, not mathematical constants
        assertThat(input.getAcronym()).doesNotContain("π", "e", "g");
        assertThat(input.getName()).contains("Input");
    }

    @Test
    @DisplayName("Should support measurement unit change")
    void shouldSupportMeasurementUnitChange() {
        // Given
        MeasurementUnitEntity unit1 = new MeasurementUnitEntity();
        unit1.setId(2L);
        unit1.setName("Metros");
        MeasurementUnitEntity unit2 = new MeasurementUnitEntity();
        unit2.setId(3L);
        unit2.setName("Centímetros");

        InputEntity input = new InputEntity();
        input.setMeasurementUnit(unit1);

        // When
        input.setMeasurementUnit(unit2);

        // Then
        assertThat(input.getMeasurementUnit()).isEqualTo(unit2);
    }

    @Test
    @DisplayName("Should support instrument reassignment")
    void shouldSupportInstrumentReassignment() {
        // Given
        InstrumentEntity instrument2 = new InstrumentEntity();
        instrument2.setId(2L);
        instrument2.setName("Instrumento 2");

        InputEntity input = new InputEntity();
        input.setInstrument(instrument);

        // When
        input.setInstrument(instrument2);

        // Then
        assertThat(input.getInstrument()).isEqualTo(instrument2);
    }
}
