package com.geosegbar.unit.entities;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class StatisticalLimitEntityTest extends BaseUnitTest {

    private OutputEntity output;

    @BeforeEach
    void setUp() {
        output = new OutputEntity();
        output.setId(1L);
    }

    @Test
    @DisplayName("Should create statistical limit with all required fields")
    void shouldCreateStatisticalLimitWithAllRequiredFields() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setId(1L);
        statisticalLimit.setOutput(output);

        assertThat(statisticalLimit).satisfies(sl -> {
            assertThat(sl.getId()).isEqualTo(1L);
            assertThat(sl.getOutput()).isEqualTo(output);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity(
                1L,
                10.5,
                90.5,
                output
        );

        assertThat(statisticalLimit.getId()).isEqualTo(1L);
        assertThat(statisticalLimit.getLowerValue()).isEqualTo(10.5);
        assertThat(statisticalLimit.getUpperValue()).isEqualTo(90.5);
        assertThat(statisticalLimit.getOutput()).isEqualTo(output);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with Output")
    void shouldMaintainOneToOneRelationshipWithOutput() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setOutput(output);

        assertThat(statisticalLimit.getOutput())
                .isNotNull()
                .isEqualTo(output);
    }

    @Test
    @DisplayName("Should support optional lower value")
    void shouldSupportOptionalLowerValue() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(15.0);

        assertThat(statisticalLimit.getLowerValue()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Should allow null lower value")
    void shouldAllowNullLowerValue() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(null);

        assertThat(statisticalLimit.getLowerValue()).isNull();
    }

    @Test
    @DisplayName("Should support optional upper value")
    void shouldSupportOptionalUpperValue() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setUpperValue(85.0);

        assertThat(statisticalLimit.getUpperValue()).isEqualTo(85.0);
    }

    @Test
    @DisplayName("Should allow null upper value")
    void shouldAllowNullUpperValue() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setUpperValue(null);

        assertThat(statisticalLimit.getUpperValue()).isNull();
    }

    @Test
    @DisplayName("Should support both lower and upper values set")
    void shouldSupportBothLowerAndUpperValuesSet() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(10.0);
        statisticalLimit.setUpperValue(90.0);

        assertThat(statisticalLimit.getLowerValue()).isEqualTo(10.0);
        assertThat(statisticalLimit.getUpperValue()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("Should support only lower value set")
    void shouldSupportOnlyLowerValueSet() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(5.0);
        statisticalLimit.setUpperValue(null);

        assertThat(statisticalLimit.getLowerValue()).isEqualTo(5.0);
        assertThat(statisticalLimit.getUpperValue()).isNull();
    }

    @Test
    @DisplayName("Should support only upper value set")
    void shouldSupportOnlyUpperValueSet() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(null);
        statisticalLimit.setUpperValue(95.0);

        assertThat(statisticalLimit.getLowerValue()).isNull();
        assertThat(statisticalLimit.getUpperValue()).isEqualTo(95.0);
    }

    @Test
    @DisplayName("Should support positive values")
    void shouldSupportPositiveValues() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(100.0);
        statisticalLimit.setUpperValue(200.0);

        assertThat(statisticalLimit.getLowerValue()).isPositive();
        assertThat(statisticalLimit.getUpperValue()).isPositive();
    }

    @Test
    @DisplayName("Should support negative values")
    void shouldSupportNegativeValues() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(-50.0);
        statisticalLimit.setUpperValue(-10.0);

        assertThat(statisticalLimit.getLowerValue()).isNegative();
        assertThat(statisticalLimit.getUpperValue()).isNegative();
    }

    @Test
    @DisplayName("Should support zero values")
    void shouldSupportZeroValues() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(0.0);
        statisticalLimit.setUpperValue(0.0);

        assertThat(statisticalLimit.getLowerValue()).isZero();
        assertThat(statisticalLimit.getUpperValue()).isZero();
    }

    @Test
    @DisplayName("Should support high precision decimal values")
    void shouldSupportHighPrecisionDecimalValues() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(12.345678901234);
        statisticalLimit.setUpperValue(87.654321098765);

        assertThat(statisticalLimit.getLowerValue()).isEqualTo(12.345678901234);
        assertThat(statisticalLimit.getUpperValue()).isEqualTo(87.654321098765);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setId(1L);
        statisticalLimit.setLowerValue(10.0);
        statisticalLimit.setUpperValue(90.0);

        Long originalId = statisticalLimit.getId();

        statisticalLimit.setLowerValue(15.0);
        statisticalLimit.setUpperValue(85.0);

        assertThat(statisticalLimit.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support wide range of values")
    void shouldSupportWideRangeOfValues() {

        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
        statisticalLimit.setLowerValue(-1000.0);
        statisticalLimit.setUpperValue(1000.0);

        assertThat(statisticalLimit.getUpperValue() - statisticalLimit.getLowerValue())
                .isEqualTo(2000.0);
    }
}
