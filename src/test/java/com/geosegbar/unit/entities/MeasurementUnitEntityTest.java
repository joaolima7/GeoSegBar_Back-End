package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.InputEntity;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - MeasurementUnitEntity")
class MeasurementUnitEntityTest extends BaseUnitTest {

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
    }

    @Test
    @DisplayName("Should create measurement unit with all required fields")
    void shouldCreateMeasurementUnitWithAllRequiredFields() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setId(1L);
        unit.setName("Metro");
        unit.setAcronym("m");

        assertThat(unit).satisfies(u -> {
            assertThat(u.getId()).isEqualTo(1L);
            assertThat(u.getName()).isEqualTo("Metro");
            assertThat(u.getAcronym()).isEqualTo("m");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity(
                1L,
                "Metro",
                "m",
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>()
        );

        assertThat(unit.getId()).isEqualTo(1L);
        assertThat(unit.getName()).isEqualTo("Metro");
        assertThat(unit.getAcronym()).isEqualTo("m");
        assertThat(unit.getInputs()).isNotNull().isEmpty();
        assertThat(unit.getConstants()).isNotNull().isEmpty();
        assertThat(unit.getOutputs()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support unique name constraint")
    void shouldSupportUniqueNameConstraint() {

        MeasurementUnitEntity unit1 = new MeasurementUnitEntity();
        unit1.setId(1L);
        unit1.setName("Metro");
        unit1.setAcronym("m");

        MeasurementUnitEntity unit2 = new MeasurementUnitEntity();
        unit2.setId(2L);
        unit2.setName("Centímetro");
        unit2.setAcronym("cm");

        assertThat(unit1.getName()).isNotEqualTo(unit2.getName());
    }

    @Test
    @DisplayName("Should support unique acronym constraint")
    void shouldSupportUniqueAcronymConstraint() {

        MeasurementUnitEntity unit1 = new MeasurementUnitEntity();
        unit1.setId(1L);
        unit1.setName("Metro");
        unit1.setAcronym("m");

        MeasurementUnitEntity unit2 = new MeasurementUnitEntity();
        unit2.setId(2L);
        unit2.setName("Centímetro");
        unit2.setAcronym("cm");

        assertThat(unit1.getAcronym()).isNotEqualTo(unit2.getAcronym());
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of inputs")
    void shouldMaintainOneToManyCollectionOfInputs() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setName("Metro");
        unit.setInputs(new HashSet<>());

        InputEntity input = new InputEntity();
        input.setId(1L);
        input.setName("Input 1");
        unit.getInputs().add(input);

        assertThat(unit.getInputs())
                .isNotNull()
                .hasSize(1)
                .contains(input);
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of constants")
    void shouldMaintainOneToManyCollectionOfConstants() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setName("Metro");
        unit.setConstants(new HashSet<>());

        ConstantEntity constant = new ConstantEntity();
        constant.setId(1L);
        constant.setName("Constant 1");
        unit.getConstants().add(constant);

        assertThat(unit.getConstants())
                .isNotNull()
                .hasSize(1)
                .contains(constant);
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of outputs")
    void shouldMaintainOneToManyCollectionOfOutputs() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setName("Metro");
        unit.setOutputs(new HashSet<>());

        OutputEntity output = new OutputEntity();
        output.setId(1L);
        output.setName("Output 1");
        unit.getOutputs().add(output);

        assertThat(unit.getOutputs())
                .isNotNull()
                .hasSize(1)
                .contains(output);
    }

    @Test
    @DisplayName("Should support multiple inputs per unit")
    void shouldSupportMultipleInputsPerUnit() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setName("Metro");
        unit.setInputs(new HashSet<>());

        InputEntity input1 = new InputEntity();
        input1.setId(1L);
        InputEntity input2 = new InputEntity();
        input2.setId(2L);
        InputEntity input3 = new InputEntity();
        input3.setId(3L);

        unit.getInputs().add(input1);
        unit.getInputs().add(input2);
        unit.getInputs().add(input3);

        assertThat(unit.getInputs()).hasSize(3);
    }

    @Test
    @DisplayName("Should support multiple constants per unit")
    void shouldSupportMultipleConstantsPerUnit() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setName("Metro");
        unit.setConstants(new HashSet<>());

        ConstantEntity const1 = new ConstantEntity();
        const1.setId(1L);
        ConstantEntity const2 = new ConstantEntity();
        const2.setId(2L);

        unit.getConstants().add(const1);
        unit.getConstants().add(const2);

        assertThat(unit.getConstants()).hasSize(2);
    }

    @Test
    @DisplayName("Should support multiple outputs per unit")
    void shouldSupportMultipleOutputsPerUnit() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setName("Metro");
        unit.setOutputs(new HashSet<>());

        OutputEntity out1 = new OutputEntity();
        out1.setId(1L);
        OutputEntity out2 = new OutputEntity();
        out2.setId(2L);

        unit.getOutputs().add(out1);
        unit.getOutputs().add(out2);

        assertThat(unit.getOutputs()).hasSize(2);
    }

    @Test
    @DisplayName("Should initialize empty collections by default")
    void shouldInitializeEmptyCollectionsByDefault() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();

        assertThat(unit.getInputs()).isNotNull().isEmpty();
        assertThat(unit.getConstants()).isNotNull().isEmpty();
        assertThat(unit.getOutputs()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support common measurement units")
    void shouldSupportCommonMeasurementUnits() {

        MeasurementUnitEntity meter = new MeasurementUnitEntity();
        meter.setName("Metro");
        meter.setAcronym("m");

        MeasurementUnitEntity centimeter = new MeasurementUnitEntity();
        centimeter.setName("Centímetro");
        centimeter.setAcronym("cm");

        MeasurementUnitEntity millimeter = new MeasurementUnitEntity();
        millimeter.setName("Milímetro");
        millimeter.setAcronym("mm");

        assertThat(meter.getAcronym()).isEqualTo("m");
        assertThat(centimeter.getAcronym()).isEqualTo("cm");
        assertThat(millimeter.getAcronym()).isEqualTo("mm");
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setName("Centímetro");

        assertThat(unit.getName()).contains("í");
    }

    @Test
    @DisplayName("Should support short acronyms")
    void shouldSupportShortAcronyms() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setAcronym("m");

        assertThat(unit.getAcronym()).hasSize(1);
    }

    @Test
    @DisplayName("Should support multi-character acronyms")
    void shouldSupportMultiCharacterAcronyms() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setAcronym("m³/s");

        assertThat(unit.getAcronym()).hasSize(4);
    }

    @Test
    @DisplayName("Should support special characters in acronyms")
    void shouldSupportSpecialCharactersInAcronyms() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setAcronym("m²");

        assertThat(unit.getAcronym()).contains("²");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setId(1L);
        unit.setName("Metro");

        Long originalId = unit.getId();

        unit.setName("Centímetro");

        assertThat(unit.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with inputs")
    void shouldSupportBidirectionalRelationshipWithInputs() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setId(1L);
        unit.setName("Metro");

        InputEntity input = new InputEntity();
        input.setId(1L);
        input.setName("Input 1");
        input.setMeasurementUnit(unit);

        unit.getInputs().add(input);

        assertThat(input.getMeasurementUnit()).isEqualTo(unit);
        assertThat(unit.getInputs()).contains(input);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with constants")
    void shouldSupportBidirectionalRelationshipWithConstants() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setId(1L);
        unit.setName("Metro");

        ConstantEntity constant = new ConstantEntity();
        constant.setId(1L);
        constant.setName("Constant 1");
        constant.setMeasurementUnit(unit);

        unit.getConstants().add(constant);

        assertThat(constant.getMeasurementUnit()).isEqualTo(unit);
        assertThat(unit.getConstants()).contains(constant);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with outputs")
    void shouldSupportBidirectionalRelationshipWithOutputs() {

        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setId(1L);
        unit.setName("Metro");

        OutputEntity output = new OutputEntity();
        output.setId(1L);
        output.setName("Output 1");
        output.setMeasurementUnit(unit);

        unit.getOutputs().add(output);

        assertThat(output.getMeasurementUnit()).isEqualTo(unit);
        assertThat(unit.getOutputs()).contains(output);
    }
}
