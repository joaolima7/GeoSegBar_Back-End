package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentGraphCustomizationPropertiesEntity")
class InstrumentGraphCustomizationPropertiesEntityTest extends BaseUnitTest {

    private InstrumentGraphPatternEntity pattern;
    private OutputEntity output;
    private StatisticalLimitEntity statisticalLimit;
    private DeterministicLimitEntity deterministicLimit;
    private ConstantEntity constant;
    private InstrumentEntity instrument;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        pattern = new InstrumentGraphPatternEntity();
        pattern.setId(1L);
        pattern.setName("Pattern 1");

        output = new OutputEntity();
        output.setId(1L);

        statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setId(1L);

        deterministicLimit = new DeterministicLimitEntity();
        deterministicLimit.setId(1L);

        constant = new ConstantEntity();
        constant.setId(1L);
        constant.setAcronym("π");

        instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("Instrumento 1");
    }

    @Test
    @DisplayName("Should create customization properties with all required fields")
    void shouldCreateCustomizationPropertiesWithAllRequiredFields() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setId(1L);
        properties.setPattern(pattern);
        properties.setCustomizationType(CustomizationTypeEnum.OUTPUT);
        properties.setLabelEnable(true);
        properties.setIsPrimaryOrdinate(true);

        // Then
        assertThat(properties).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getPattern()).isEqualTo(pattern);
            assertThat(p.getCustomizationType()).isEqualTo(CustomizationTypeEnum.OUTPUT);
            assertThat(p.getLabelEnable()).isTrue();
            assertThat(p.getIsPrimaryOrdinate()).isTrue();
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity(
                1L,
                "Output 1",
                pattern,
                CustomizationTypeEnum.OUTPUT,
                "#FF5733",
                LineTypeEnum.SOLID,
                true,
                true,
                statisticalLimit,
                deterministicLimit,
                output,
                constant,
                instrument,
                LimitValueTypeEnum.STATISTICAL_UPPER
        );

        // Then
        assertThat(properties.getId()).isEqualTo(1L);
        assertThat(properties.getName()).isEqualTo("Output 1");
        assertThat(properties.getPattern()).isEqualTo(pattern);
        assertThat(properties.getCustomizationType()).isEqualTo(CustomizationTypeEnum.OUTPUT);
        assertThat(properties.getFillColor()).isEqualTo("#FF5733");
        assertThat(properties.getLineType()).isEqualTo(LineTypeEnum.SOLID);
        assertThat(properties.getLabelEnable()).isTrue();
        assertThat(properties.getIsPrimaryOrdinate()).isTrue();
        assertThat(properties.getStatisticalLimit()).isEqualTo(statisticalLimit);
        assertThat(properties.getDeterministicLimit()).isEqualTo(deterministicLimit);
        assertThat(properties.getOutput()).isEqualTo(output);
        assertThat(properties.getConstant()).isEqualTo(constant);
        assertThat(properties.getInstrument()).isEqualTo(instrument);
        assertThat(properties.getLimitValueType()).isEqualTo(LimitValueTypeEnum.STATISTICAL_UPPER);
    }

    @Test
    @DisplayName("Should default labelEnable to false")
    void shouldDefaultLabelEnableToFalse() {
        // Given & When
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();

        // Then
        assertThat(properties.getLabelEnable()).isFalse();
    }

    @Test
    @DisplayName("Should default isPrimaryOrdinate to true")
    void shouldDefaultIsPrimaryOrdinateToTrue() {
        // Given & When
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();

        // Then
        assertThat(properties.getIsPrimaryOrdinate()).isTrue();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with InstrumentGraphPattern")
    void shouldMaintainManyToOneRelationshipWithInstrumentGraphPattern() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setPattern(pattern);

        // Then
        assertThat(properties.getPattern())
                .isNotNull()
                .isEqualTo(pattern);
    }

    @Test
    @DisplayName("Should support OUTPUT customization type")
    void shouldSupportOutputCustomizationType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setCustomizationType(CustomizationTypeEnum.OUTPUT);
        properties.setOutput(output);

        // Then
        assertThat(properties.getCustomizationType()).isEqualTo(CustomizationTypeEnum.OUTPUT);
        assertThat(properties.getOutput()).isNotNull();
    }

    @Test
    @DisplayName("Should support INSTRUMENT customization type")
    void shouldSupportInstrumentCustomizationType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setCustomizationType(CustomizationTypeEnum.INSTRUMENT);
        properties.setInstrument(instrument);

        // Then
        assertThat(properties.getCustomizationType()).isEqualTo(CustomizationTypeEnum.INSTRUMENT);
        assertThat(properties.getInstrument()).isNotNull();
    }

    @Test
    @DisplayName("Should support STATISTICAL_LIMIT customization type")
    void shouldSupportStatisticalLimitCustomizationType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setCustomizationType(CustomizationTypeEnum.STATISTICAL_LIMIT);
        properties.setStatisticalLimit(statisticalLimit);

        // Then
        assertThat(properties.getCustomizationType()).isEqualTo(CustomizationTypeEnum.STATISTICAL_LIMIT);
        assertThat(properties.getStatisticalLimit()).isNotNull();
    }

    @Test
    @DisplayName("Should support DETERMINISTIC_LIMIT customization type")
    void shouldSupportDeterministicLimitCustomizationType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setCustomizationType(CustomizationTypeEnum.DETERMINISTIC_LIMIT);
        properties.setDeterministicLimit(deterministicLimit);

        // Then
        assertThat(properties.getCustomizationType()).isEqualTo(CustomizationTypeEnum.DETERMINISTIC_LIMIT);
        assertThat(properties.getDeterministicLimit()).isNotNull();
    }

    @Test
    @DisplayName("Should support CONSTANT customization type")
    void shouldSupportConstantCustomizationType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setCustomizationType(CustomizationTypeEnum.CONSTANT);
        properties.setConstant(constant);

        // Then
        assertThat(properties.getCustomizationType()).isEqualTo(CustomizationTypeEnum.CONSTANT);
        assertThat(properties.getConstant()).isNotNull();
    }

    @Test
    @DisplayName("Should support LINIMETRIC_RULER customization type")
    void shouldSupportLinimetricRulerCustomizationType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setCustomizationType(CustomizationTypeEnum.LINIMETRIC_RULER);

        // Then
        assertThat(properties.getCustomizationType()).isEqualTo(CustomizationTypeEnum.LINIMETRIC_RULER);
    }

    @Test
    @DisplayName("Should validate hex color format with 6 characters")
    void shouldValidateHexColorFormatWithSixCharacters() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setFillColor("#FF5733");

        // Then
        assertThat(properties.getFillColor())
                .matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }

    @Test
    @DisplayName("Should validate hex color format with 3 characters")
    void shouldValidateHexColorFormatWithThreeCharacters() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setFillColor("#F57");

        // Then
        assertThat(properties.getFillColor())
                .matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }

    @Test
    @DisplayName("Should support lowercase hex color")
    void shouldSupportLowercaseHexColor() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setFillColor("#ff5733");

        // Then
        assertThat(properties.getFillColor()).isEqualTo("#ff5733");
    }

    @Test
    @DisplayName("Should support uppercase hex color")
    void shouldSupportUppercaseHexColor() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setFillColor("#FF5733");

        // Then
        assertThat(properties.getFillColor()).isEqualTo("#FF5733");
    }

    @Test
    @DisplayName("Should allow null fill color")
    void shouldAllowNullFillColor() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setFillColor(null);

        // Then
        assertThat(properties.getFillColor()).isNull();
    }

    @Test
    @DisplayName("Should support SOLID line type")
    void shouldSupportSolidLineType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLineType(LineTypeEnum.SOLID);

        // Then
        assertThat(properties.getLineType()).isEqualTo(LineTypeEnum.SOLID);
    }

    @Test
    @DisplayName("Should support DASHED line type")
    void shouldSupportDashedLineType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLineType(LineTypeEnum.DASHED);

        // Then
        assertThat(properties.getLineType()).isEqualTo(LineTypeEnum.DASHED);
    }

    @Test
    @DisplayName("Should support DOTTED line type")
    void shouldSupportDottedLineType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLineType(LineTypeEnum.DOTTED);

        // Then
        assertThat(properties.getLineType()).isEqualTo(LineTypeEnum.DOTTED);
    }

    @Test
    @DisplayName("Should support DASH_DOT line type")
    void shouldSupportDashDotLineType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLineType(LineTypeEnum.DASH_DOT);

        // Then
        assertThat(properties.getLineType()).isEqualTo(LineTypeEnum.DASH_DOT);
    }

    @Test
    @DisplayName("Should support DASH_DOT_DOT line type")
    void shouldSupportDashDotDotLineType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLineType(LineTypeEnum.DASH_DOT_DOT);

        // Then
        assertThat(properties.getLineType()).isEqualTo(LineTypeEnum.DASH_DOT_DOT);
    }

    @Test
    @DisplayName("Should enable label")
    void shouldEnableLabel() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLabelEnable(true);

        // Then
        assertThat(properties.getLabelEnable()).isTrue();
    }

    @Test
    @DisplayName("Should disable label")
    void shouldDisableLabel() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLabelEnable(false);

        // Then
        assertThat(properties.getLabelEnable()).isFalse();
    }

    @Test
    @DisplayName("Should set to primary ordinate")
    void shouldSetToPrimaryOrdinate() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setIsPrimaryOrdinate(true);

        // Then
        assertThat(properties.getIsPrimaryOrdinate()).isTrue();
    }

    @Test
    @DisplayName("Should set to secondary ordinate")
    void shouldSetToSecondaryOrdinate() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setIsPrimaryOrdinate(false);

        // Then
        assertThat(properties.getIsPrimaryOrdinate()).isFalse();
    }

    @Test
    @DisplayName("Should support STATISTICAL_LOWER limit value type")
    void shouldSupportStatisticalLowerLimitValueType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLimitValueType(LimitValueTypeEnum.STATISTICAL_LOWER);

        // Then
        assertThat(properties.getLimitValueType()).isEqualTo(LimitValueTypeEnum.STATISTICAL_LOWER);
    }

    @Test
    @DisplayName("Should support STATISTICAL_UPPER limit value type")
    void shouldSupportStatisticalUpperLimitValueType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLimitValueType(LimitValueTypeEnum.STATISTICAL_UPPER);

        // Then
        assertThat(properties.getLimitValueType()).isEqualTo(LimitValueTypeEnum.STATISTICAL_UPPER);
    }

    @Test
    @DisplayName("Should support DETERMINISTIC_ATTENTION limit value type")
    void shouldSupportDeterministicAttentionLimitValueType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLimitValueType(LimitValueTypeEnum.DETERMINISTIC_ATTENTION);

        // Then
        assertThat(properties.getLimitValueType()).isEqualTo(LimitValueTypeEnum.DETERMINISTIC_ATTENTION);
    }

    @Test
    @DisplayName("Should support DETERMINISTIC_ALERT limit value type")
    void shouldSupportDeterministicAlertLimitValueType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLimitValueType(LimitValueTypeEnum.DETERMINISTIC_ALERT);

        // Then
        assertThat(properties.getLimitValueType()).isEqualTo(LimitValueTypeEnum.DETERMINISTIC_ALERT);
    }

    @Test
    @DisplayName("Should support DETERMINISTIC_EMERGENCY limit value type")
    void shouldSupportDeterministicEmergencyLimitValueType() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setLimitValueType(LimitValueTypeEnum.DETERMINISTIC_EMERGENCY);

        // Then
        assertThat(properties.getLimitValueType()).isEqualTo(LimitValueTypeEnum.DETERMINISTIC_EMERGENCY);
    }

    @Test
    @DisplayName("Should allow optional name")
    void shouldAllowOptionalName() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setName("Custom Name");

        // Then
        assertThat(properties.getName()).isEqualTo("Custom Name");
    }

    @Test
    @DisplayName("Should allow null name")
    void shouldAllowNullName() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setName(null);

        // Then
        assertThat(properties.getName()).isNull();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setId(1L);
        properties.setLabelEnable(false);

        Long originalId = properties.getId();

        // When
        properties.setLabelEnable(true);
        properties.setFillColor("#FF5733");

        // Then
        assertThat(properties.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support complete customization configuration")
    void shouldSupportCompleteCustomizationConfiguration() {
        // Given
        InstrumentGraphCustomizationPropertiesEntity properties = new InstrumentGraphCustomizationPropertiesEntity();
        properties.setPattern(pattern);
        properties.setCustomizationType(CustomizationTypeEnum.OUTPUT);
        properties.setOutput(output);
        properties.setFillColor("#3498db");
        properties.setLineType(LineTypeEnum.SOLID);
        properties.setLabelEnable(true);
        properties.setIsPrimaryOrdinate(true);
        properties.setLimitValueType(LimitValueTypeEnum.DETERMINISTIC_ALERT);

        // Then
        assertThat(properties.getPattern()).isNotNull();
        assertThat(properties.getCustomizationType()).isEqualTo(CustomizationTypeEnum.OUTPUT);
        assertThat(properties.getOutput()).isNotNull();
        assertThat(properties.getFillColor()).matches("^#([A-Fa-f0-9]{6})$");
        assertThat(properties.getLineType()).isNotNull();
        assertThat(properties.getLabelEnable()).isTrue();
        assertThat(properties.getIsPrimaryOrdinate()).isTrue();
        assertThat(properties.getLimitValueType()).isNotNull();
    }
}
