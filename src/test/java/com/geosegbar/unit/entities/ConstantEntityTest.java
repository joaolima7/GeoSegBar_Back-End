package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - ConstantEntity")
class ConstantEntityTest extends BaseUnitTest {

    private MeasurementUnitEntity measurementUnit;
    private InstrumentEntity instrument;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        measurementUnit = new MeasurementUnitEntity();
        measurementUnit.setId(1L);
        measurementUnit.setName("Metro");
        measurementUnit.setAcronym("m");

        instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("Instrumento 1");
    }

    @Test
    @DisplayName("Should create constant with all required fields")
    void shouldCreateConstantWithAllRequiredFields() {

        ConstantEntity constant = new ConstantEntity();
        constant.setId(1L);
        constant.setAcronym("K");
        constant.setName("Constante de Permeabilidade");
        constant.setPrecision(2);
        constant.setValue(9.81);
        constant.setMeasurementUnit(measurementUnit);
        constant.setInstrument(instrument);

        assertThat(constant).satisfies(c -> {
            assertThat(c.getId()).isEqualTo(1L);
            assertThat(c.getAcronym()).isEqualTo("K");
            assertThat(c.getName()).isEqualTo("Constante de Permeabilidade");
            assertThat(c.getPrecision()).isEqualTo(2);
            assertThat(c.getValue()).isEqualTo(9.81);
            assertThat(c.getMeasurementUnit()).isEqualTo(measurementUnit);
            assertThat(c.getInstrument()).isEqualTo(instrument);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        ConstantEntity constant = new ConstantEntity(
                1L,
                "G",
                "Gravidade",
                3,
                9.807,
                measurementUnit,
                instrument
        );

        assertThat(constant.getId()).isEqualTo(1L);
        assertThat(constant.getAcronym()).isEqualTo("G");
        assertThat(constant.getName()).isEqualTo("Gravidade");
        assertThat(constant.getPrecision()).isEqualTo(3);
        assertThat(constant.getValue()).isEqualTo(9.807);
        assertThat(constant.getMeasurementUnit()).isNotNull();
        assertThat(constant.getInstrument()).isNotNull();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with MeasurementUnit")
    void shouldMaintainManyToOneRelationshipWithMeasurementUnit() {

        ConstantEntity constant = new ConstantEntity();
        constant.setMeasurementUnit(measurementUnit);

        assertThat(constant.getMeasurementUnit())
                .isNotNull()
                .isEqualTo(measurementUnit);
        assertThat(constant.getMeasurementUnit().getAcronym()).isEqualTo("m");
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Instrument")
    void shouldMaintainManyToOneRelationshipWithInstrument() {

        ConstantEntity constant = new ConstantEntity();
        constant.setInstrument(instrument);

        assertThat(constant.getInstrument())
                .isNotNull()
                .isEqualTo(instrument);
    }

    @Test
    @DisplayName("Should handle different precision values")
    void shouldHandleDifferentPrecisionValues() {

        ConstantEntity constant1 = new ConstantEntity();
        constant1.setPrecision(0);

        ConstantEntity constant2 = new ConstantEntity();
        constant2.setPrecision(5);

        ConstantEntity constant3 = new ConstantEntity();
        constant3.setPrecision(10);

        assertThat(constant1.getPrecision()).isEqualTo(0);
        assertThat(constant2.getPrecision()).isEqualTo(5);
        assertThat(constant3.getPrecision()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should handle positive and negative values")
    void shouldHandlePositiveAndNegativeValues() {

        ConstantEntity constant1 = new ConstantEntity();
        constant1.setValue(100.5);

        ConstantEntity constant2 = new ConstantEntity();
        constant2.setValue(-50.25);

        ConstantEntity constant3 = new ConstantEntity();
        constant3.setValue(0.0);

        assertThat(constant1.getValue()).isPositive();
        assertThat(constant2.getValue()).isNegative();
        assertThat(constant3.getValue()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle very small decimal values")
    void shouldHandleVerySmallDecimalValues() {

        ConstantEntity constant = new ConstantEntity();
        constant.setValue(0.000001);
        constant.setPrecision(6);

        assertThat(constant.getValue()).isEqualTo(0.000001);
        assertThat(constant.getPrecision()).isEqualTo(6);
    }

    @Test
    @DisplayName("Should handle very large values")
    void shouldHandleVeryLargeValues() {

        ConstantEntity constant = new ConstantEntity();
        constant.setValue(999999999.99);

        assertThat(constant.getValue()).isEqualTo(999999999.99);
    }

    @Test
    @DisplayName("Should handle different acronym formats")
    void shouldHandleDifferentAcronymFormats() {

        ConstantEntity constant1 = new ConstantEntity();
        constant1.setAcronym("K");

        ConstantEntity constant2 = new ConstantEntity();
        constant2.setAcronym("PI");

        ConstantEntity constant3 = new ConstantEntity();
        constant3.setAcronym("DELTA_H");

        assertThat(constant1.getAcronym()).hasSize(1);
        assertThat(constant2.getAcronym()).hasSize(2);
        assertThat(constant3.getAcronym()).contains("_");
    }

    @Test
    @DisplayName("Should handle constant names with special characters")
    void shouldHandleConstantNamesWithSpecialCharacters() {

        ConstantEntity constant = new ConstantEntity();
        constant.setName("Constante α (Alpha)");

        assertThat(constant.getName())
                .contains("α")
                .contains("(")
                .contains(")");
    }

    @Test
    @DisplayName("Should handle long constant names")
    void shouldHandleLongConstantNames() {

        ConstantEntity constant = new ConstantEntity();
        String longName = "Constante de Permeabilidade Hidráulica do Solo em Condições Saturadas";
        constant.setName(longName);

        assertThat(constant.getName()).isEqualTo(longName);
        assertThat(constant.getName().length()).isGreaterThan(50);
    }

    @Test
    @DisplayName("Should handle constants with same instrument but different units")
    void shouldHandleConstantsWithSameInstrumentButDifferentUnits() {

        MeasurementUnitEntity unit1 = new MeasurementUnitEntity();
        unit1.setId(1L);
        unit1.setAcronym("m");

        MeasurementUnitEntity unit2 = new MeasurementUnitEntity();
        unit2.setId(2L);
        unit2.setAcronym("cm");

        ConstantEntity constant1 = new ConstantEntity();
        constant1.setInstrument(instrument);
        constant1.setMeasurementUnit(unit1);

        ConstantEntity constant2 = new ConstantEntity();
        constant2.setInstrument(instrument);
        constant2.setMeasurementUnit(unit2);

        assertThat(constant1.getInstrument()).isEqualTo(constant2.getInstrument());
        assertThat(constant1.getMeasurementUnit()).isNotEqualTo(constant2.getMeasurementUnit());
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        ConstantEntity constant = new ConstantEntity();
        constant.setId(1L);
        constant.setAcronym("K");
        constant.setValue(10.0);

        Long originalId = constant.getId();

        constant.setAcronym("K2");
        constant.setValue(20.0);

        assertThat(constant.getId()).isEqualTo(originalId);
        assertThat(constant.getAcronym()).isEqualTo("K2");
        assertThat(constant.getValue()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Should support scientific notation values")
    void shouldSupportScientificNotationValues() {

        ConstantEntity constant = new ConstantEntity();
        constant.setValue(1.602e-19);

        assertThat(constant.getValue()).isEqualTo(1.602e-19);
    }

    @Test
    @DisplayName("Should handle Greek letter acronyms")
    void shouldHandleGreekLetterAcronyms() {

        ConstantEntity constant = new ConstantEntity();
        constant.setAcronym("π");
        constant.setName("Pi");
        constant.setValue(3.14159);

        assertThat(constant.getAcronym()).isEqualTo("π");
        assertThat(constant.getName()).isEqualTo("Pi");
    }

    @Test
    @DisplayName("Should handle zero precision")
    void shouldHandleZeroPrecision() {

        ConstantEntity constant = new ConstantEntity();
        constant.setPrecision(0);
        constant.setValue(10.0);

        assertThat(constant.getPrecision()).isEqualTo(0);
        assertThat(constant.getValue()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should support constants related to same measurement unit")
    void shouldSupportConstantsRelatedToSameMeasurementUnit() {

        ConstantEntity constant1 = new ConstantEntity();
        constant1.setAcronym("K1");
        constant1.setMeasurementUnit(measurementUnit);

        ConstantEntity constant2 = new ConstantEntity();
        constant2.setAcronym("K2");
        constant2.setMeasurementUnit(measurementUnit);

        assertThat(constant1.getMeasurementUnit()).isEqualTo(constant2.getMeasurementUnit());
        assertThat(constant1.getAcronym()).isNotEqualTo(constant2.getAcronym());
    }
}
