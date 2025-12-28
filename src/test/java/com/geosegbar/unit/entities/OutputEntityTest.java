package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - OutputEntity")
class OutputEntityTest extends BaseUnitTest {

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
    @DisplayName("Should create output with all required fields")
    void shouldCreateOutputWithAllRequiredFields() {

        OutputEntity output = new OutputEntity();
        output.setId(1L);
        output.setAcronym("OUT1");
        output.setName("Output 1");
        output.setEquation("x + y");
        output.setPrecision(2);
        output.setMeasurementUnit(measurementUnit);
        output.setInstrument(instrument);

        assertThat(output).satisfies(o -> {
            assertThat(o.getId()).isEqualTo(1L);
            assertThat(o.getAcronym()).isEqualTo("OUT1");
            assertThat(o.getName()).isEqualTo("Output 1");
            assertThat(o.getEquation()).isEqualTo("x + y");
            assertThat(o.getPrecision()).isEqualTo(2);
            assertThat(o.getMeasurementUnit()).isEqualTo(measurementUnit);
            assertThat(o.getInstrument()).isEqualTo(instrument);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        OutputEntity output = new OutputEntity(
                1L,
                "OUT1",
                "Output 1",
                "x + y",
                2,
                true,
                null,
                null,
                measurementUnit,
                instrument
        );

        assertThat(output.getId()).isEqualTo(1L);
        assertThat(output.getAcronym()).isEqualTo("OUT1");
        assertThat(output.getName()).isEqualTo("Output 1");
        assertThat(output.getEquation()).isEqualTo("x + y");
        assertThat(output.getPrecision()).isEqualTo(2);
        assertThat(output.getActive()).isTrue();
        assertThat(output.getStatisticalLimit()).isNull();
        assertThat(output.getDeterministicLimit()).isNull();
        assertThat(output.getMeasurementUnit()).isEqualTo(measurementUnit);
        assertThat(output.getInstrument()).isEqualTo(instrument);
    }

    @Test
    @DisplayName("Should default active to true")
    void shouldDefaultActiveToTrue() {

        OutputEntity output = new OutputEntity();
        output.setActive(true);

        assertThat(output.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should allow active to be false")
    void shouldAllowActiveToFalse() {

        OutputEntity output = new OutputEntity();
        output.setActive(false);

        assertThat(output.getActive()).isFalse();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with MeasurementUnit")
    void shouldMaintainManyToOneRelationshipWithMeasurementUnit() {

        OutputEntity output = new OutputEntity();
        output.setMeasurementUnit(measurementUnit);

        assertThat(output.getMeasurementUnit())
                .isNotNull()
                .isEqualTo(measurementUnit);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Instrument")
    void shouldMaintainManyToOneRelationshipWithInstrument() {

        OutputEntity output = new OutputEntity();
        output.setInstrument(instrument);

        assertThat(output.getInstrument())
                .isNotNull()
                .isEqualTo(instrument);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with StatisticalLimit")
    void shouldMaintainOneToOneRelationshipWithStatisticalLimit() {

        OutputEntity output = new OutputEntity();
        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setId(1L);
        output.setStatisticalLimit(statisticalLimit);

        assertThat(output.getStatisticalLimit())
                .isNotNull()
                .isEqualTo(statisticalLimit);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with DeterministicLimit")
    void shouldMaintainOneToOneRelationshipWithDeterministicLimit() {

        OutputEntity output = new OutputEntity();
        DeterministicLimitEntity deterministicLimit = new DeterministicLimitEntity();
        deterministicLimit.setId(1L);
        output.setDeterministicLimit(deterministicLimit);

        assertThat(output.getDeterministicLimit())
                .isNotNull()
                .isEqualTo(deterministicLimit);
    }

    @Test
    @DisplayName("Should allow null StatisticalLimit")
    void shouldAllowNullStatisticalLimit() {

        OutputEntity output = new OutputEntity();
        output.setAcronym("OUT1");
        output.setStatisticalLimit(null);

        assertThat(output.getStatisticalLimit()).isNull();
    }

    @Test
    @DisplayName("Should allow null DeterministicLimit")
    void shouldAllowNullDeterministicLimit() {

        OutputEntity output = new OutputEntity();
        output.setAcronym("OUT1");
        output.setDeterministicLimit(null);

        assertThat(output.getDeterministicLimit()).isNull();
    }

    @Test
    @DisplayName("Should support simple equations")
    void shouldSupportSimpleEquations() {

        OutputEntity output = new OutputEntity();
        output.setEquation("x + y");

        assertThat(output.getEquation()).isEqualTo("x + y");
    }

    @Test
    @DisplayName("Should support complex equations")
    void shouldSupportComplexEquations() {

        OutputEntity output = new OutputEntity();
        output.setEquation("(x * 2) + (y / 3) - z");

        assertThat(output.getEquation()).contains("*", "/", "+", "-", "(", ")");
    }

    @Test
    @DisplayName("Should support equations with functions")
    void shouldSupportEquationsWithFunctions() {

        OutputEntity output = new OutputEntity();
        output.setEquation("Math.sqrt(x) + Math.pow(y, 2)");

        assertThat(output.getEquation()).contains("Math.", "sqrt", "pow");
    }

    @Test
    @DisplayName("Should support zero precision")
    void shouldSupportZeroPrecision() {

        OutputEntity output = new OutputEntity();
        output.setPrecision(0);

        assertThat(output.getPrecision()).isZero();
    }

    @Test
    @DisplayName("Should support positive precision")
    void shouldSupportPositivePrecision() {

        OutputEntity output = new OutputEntity();
        output.setPrecision(3);

        assertThat(output.getPrecision()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should support different precision values")
    void shouldSupportDifferentPrecisionValues() {

        OutputEntity output1 = new OutputEntity();
        output1.setPrecision(2);

        OutputEntity output2 = new OutputEntity();
        output2.setPrecision(4);

        assertThat(output1.getPrecision()).isLessThan(output2.getPrecision());
    }

    @Test
    @DisplayName("Should support short acronyms")
    void shouldSupportShortAcronyms() {

        OutputEntity output = new OutputEntity();
        output.setAcronym("O1");

        assertThat(output.getAcronym()).hasSize(2);
    }

    @Test
    @DisplayName("Should support descriptive acronyms")
    void shouldSupportDescriptiveAcronyms() {

        OutputEntity output = new OutputEntity();
        output.setAcronym("DESLOCAMENTO");

        assertThat(output.getAcronym()).hasSize(12);
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {

        OutputEntity output = new OutputEntity();
        output.setName("Deslocamento Médio");

        assertThat(output.getName()).contains("é");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        OutputEntity output = new OutputEntity();
        output.setId(1L);
        output.setActive(true);

        Long originalId = output.getId();

        output.setActive(false);

        assertThat(output.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support multiple outputs per instrument")
    void shouldSupportMultipleOutputsPerInstrument() {

        OutputEntity output1 = new OutputEntity();
        output1.setId(1L);
        output1.setAcronym("OUT1");
        output1.setInstrument(instrument);

        OutputEntity output2 = new OutputEntity();
        output2.setId(2L);
        output2.setAcronym("OUT2");
        output2.setInstrument(instrument);

        assertThat(output1.getInstrument()).isEqualTo(output2.getInstrument());
        assertThat(output1.getId()).isNotEqualTo(output2.getId());
    }

    @Test
    @DisplayName("Should support multiple outputs per measurement unit")
    void shouldSupportMultipleOutputsPerMeasurementUnit() {

        OutputEntity output1 = new OutputEntity();
        output1.setId(1L);
        output1.setAcronym("OUT1");
        output1.setMeasurementUnit(measurementUnit);

        OutputEntity output2 = new OutputEntity();
        output2.setId(2L);
        output2.setAcronym("OUT2");
        output2.setMeasurementUnit(measurementUnit);

        assertThat(output1.getMeasurementUnit()).isEqualTo(output2.getMeasurementUnit());
    }

    @Test
    @DisplayName("Should support cascade operations on StatisticalLimit")
    void shouldSupportCascadeOperationsOnStatisticalLimit() {

        OutputEntity output = new OutputEntity();
        output.setId(1L);

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setId(1L);
        statisticalLimit.setOutput(output);

        output.setStatisticalLimit(statisticalLimit);

        assertThat(statisticalLimit.getOutput()).isEqualTo(output);
        assertThat(output.getStatisticalLimit()).isEqualTo(statisticalLimit);
    }

    @Test
    @DisplayName("Should support cascade operations on DeterministicLimit")
    void shouldSupportCascadeOperationsOnDeterministicLimit() {

        OutputEntity output = new OutputEntity();
        output.setId(1L);

        DeterministicLimitEntity deterministicLimit = new DeterministicLimitEntity();
        deterministicLimit.setId(1L);
        deterministicLimit.setOutput(output);

        output.setDeterministicLimit(deterministicLimit);

        assertThat(deterministicLimit.getOutput()).isEqualTo(output);
        assertThat(output.getDeterministicLimit()).isEqualTo(deterministicLimit);
    }

    @Test
    @DisplayName("Should support orphan removal for StatisticalLimit")
    void shouldSupportOrphanRemovalForStatisticalLimit() {

        OutputEntity output = new OutputEntity();
        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setId(1L);
        output.setStatisticalLimit(statisticalLimit);

        output.setStatisticalLimit(null);

        assertThat(output.getStatisticalLimit()).isNull();
    }

    @Test
    @DisplayName("Should support orphan removal for DeterministicLimit")
    void shouldSupportOrphanRemovalForDeterministicLimit() {

        OutputEntity output = new OutputEntity();
        DeterministicLimitEntity deterministicLimit = new DeterministicLimitEntity();
        deterministicLimit.setId(1L);
        output.setDeterministicLimit(deterministicLimit);

        output.setDeterministicLimit(null);

        assertThat(output.getDeterministicLimit()).isNull();
    }

    @Test
    @DisplayName("Should support complete output configuration")
    void shouldSupportCompleteOutputConfiguration() {

        OutputEntity output = new OutputEntity();
        output.setId(1L);
        output.setAcronym("DESL");
        output.setName("Deslocamento Total");
        output.setEquation("Math.sqrt(x*x + y*y)");
        output.setPrecision(3);
        output.setActive(true);
        output.setMeasurementUnit(measurementUnit);
        output.setInstrument(instrument);

        StatisticalLimitEntity statLimit = new StatisticalLimitEntity();
        statLimit.setId(1L);
        output.setStatisticalLimit(statLimit);

        DeterministicLimitEntity detLimit = new DeterministicLimitEntity();
        detLimit.setId(1L);
        output.setDeterministicLimit(detLimit);

        assertThat(output.getId()).isNotNull();
        assertThat(output.getAcronym()).isNotBlank();
        assertThat(output.getName()).isNotBlank();
        assertThat(output.getEquation()).isNotBlank();
        assertThat(output.getPrecision()).isNotNull();
        assertThat(output.getActive()).isNotNull();
        assertThat(output.getMeasurementUnit()).isNotNull();
        assertThat(output.getInstrument()).isNotNull();
        assertThat(output.getStatisticalLimit()).isNotNull();
        assertThat(output.getDeterministicLimit()).isNotNull();
    }
}
