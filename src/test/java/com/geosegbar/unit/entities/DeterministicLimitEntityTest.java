package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - DeterministicLimitEntity")
class DeterministicLimitEntityTest extends BaseUnitTest {

    private OutputEntity output;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        output = new OutputEntity();
        output.setId(1L);
        output.setAcronym("OUT1");
    }

    @Test
    @DisplayName("Should create deterministic limit with all thresholds")
    void shouldCreateDeterministicLimitWithAllThresholds() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setId(1L);
        limit.setAttentionValue(10.0);
        limit.setAlertValue(20.0);
        limit.setEmergencyValue(30.0);
        limit.setOutput(output);

        // Then
        assertThat(limit).satisfies(l -> {
            assertThat(l.getId()).isEqualTo(1L);
            assertThat(l.getAttentionValue()).isEqualTo(10.0);
            assertThat(l.getAlertValue()).isEqualTo(20.0);
            assertThat(l.getEmergencyValue()).isEqualTo(30.0);
            assertThat(l.getOutput()).isEqualTo(output);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        DeterministicLimitEntity limit = new DeterministicLimitEntity(
                1L,
                5.0,
                15.0,
                25.0,
                output
        );

        // Then
        assertThat(limit.getId()).isEqualTo(1L);
        assertThat(limit.getAttentionValue()).isEqualTo(5.0);
        assertThat(limit.getAlertValue()).isEqualTo(15.0);
        assertThat(limit.getEmergencyValue()).isEqualTo(25.0);
        assertThat(limit.getOutput()).isNotNull();
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with Output")
    void shouldMaintainOneToOneRelationshipWithOutput() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setOutput(output);

        // Then
        assertThat(limit.getOutput())
                .isNotNull()
                .isEqualTo(output);
    }

    @Test
    @DisplayName("Should allow all threshold values to be null")
    void shouldAllowAllThresholdValuesToBeNull() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setOutput(output);
        limit.setAttentionValue(null);
        limit.setAlertValue(null);
        limit.setEmergencyValue(null);

        // Then
        assertThat(limit.getAttentionValue()).isNull();
        assertThat(limit.getAlertValue()).isNull();
        assertThat(limit.getEmergencyValue()).isNull();
        assertThat(limit.getOutput()).isNotNull();
    }

    @Test
    @DisplayName("Should handle only attention value")
    void shouldHandleOnlyAttentionValue() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(10.0);
        limit.setAlertValue(null);
        limit.setEmergencyValue(null);

        // Then
        assertThat(limit.getAttentionValue()).isEqualTo(10.0);
        assertThat(limit.getAlertValue()).isNull();
        assertThat(limit.getEmergencyValue()).isNull();
    }

    @Test
    @DisplayName("Should handle only alert value")
    void shouldHandleOnlyAlertValue() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(null);
        limit.setAlertValue(20.0);
        limit.setEmergencyValue(null);

        // Then
        assertThat(limit.getAttentionValue()).isNull();
        assertThat(limit.getAlertValue()).isEqualTo(20.0);
        assertThat(limit.getEmergencyValue()).isNull();
    }

    @Test
    @DisplayName("Should handle only emergency value")
    void shouldHandleOnlyEmergencyValue() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(null);
        limit.setAlertValue(null);
        limit.setEmergencyValue(30.0);

        // Then
        assertThat(limit.getAttentionValue()).isNull();
        assertThat(limit.getAlertValue()).isNull();
        assertThat(limit.getEmergencyValue()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Should handle progressive threshold values")
    void shouldHandleProgressiveThresholdValues() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(10.0);
        limit.setAlertValue(20.0);
        limit.setEmergencyValue(30.0);

        // Then - Values should be progressive
        assertThat(limit.getAttentionValue()).isLessThan(limit.getAlertValue());
        assertThat(limit.getAlertValue()).isLessThan(limit.getEmergencyValue());
    }

    @Test
    @DisplayName("Should handle negative threshold values")
    void shouldHandleNegativeThresholdValues() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(-10.0);
        limit.setAlertValue(-20.0);
        limit.setEmergencyValue(-30.0);

        // Then
        assertThat(limit.getAttentionValue()).isNegative();
        assertThat(limit.getAlertValue()).isNegative();
        assertThat(limit.getEmergencyValue()).isNegative();
    }

    @Test
    @DisplayName("Should handle zero threshold values")
    void shouldHandleZeroThresholdValues() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(0.0);
        limit.setAlertValue(0.0);
        limit.setEmergencyValue(0.0);

        // Then
        assertThat(limit.getAttentionValue()).isEqualTo(0.0);
        assertThat(limit.getAlertValue()).isEqualTo(0.0);
        assertThat(limit.getEmergencyValue()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle decimal threshold values")
    void shouldHandleDecimalThresholdValues() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(10.5);
        limit.setAlertValue(20.75);
        limit.setEmergencyValue(30.999);

        // Then
        assertThat(limit.getAttentionValue()).isEqualTo(10.5);
        assertThat(limit.getAlertValue()).isEqualTo(20.75);
        assertThat(limit.getEmergencyValue()).isEqualTo(30.999);
    }

    @Test
    @DisplayName("Should handle very small threshold values")
    void shouldHandleVerySmallThresholdValues() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(0.001);
        limit.setAlertValue(0.002);
        limit.setEmergencyValue(0.003);

        // Then
        assertThat(limit.getAttentionValue()).isEqualTo(0.001);
        assertThat(limit.getAlertValue()).isEqualTo(0.002);
        assertThat(limit.getEmergencyValue()).isEqualTo(0.003);
    }

    @Test
    @DisplayName("Should handle very large threshold values")
    void shouldHandleVeryLargeThresholdValues() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(1000000.0);
        limit.setAlertValue(2000000.0);
        limit.setEmergencyValue(3000000.0);

        // Then
        assertThat(limit.getAttentionValue()).isEqualTo(1000000.0);
        assertThat(limit.getAlertValue()).isEqualTo(2000000.0);
        assertThat(limit.getEmergencyValue()).isEqualTo(3000000.0);
    }

    @Test
    @DisplayName("Should update threshold values independently")
    void shouldUpdateThresholdValuesIndependently() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(10.0);
        limit.setAlertValue(20.0);
        limit.setEmergencyValue(30.0);

        // When - Update only attention value
        limit.setAttentionValue(12.0);

        // Then
        assertThat(limit.getAttentionValue()).isEqualTo(12.0);
        assertThat(limit.getAlertValue()).isEqualTo(20.0);
        assertThat(limit.getEmergencyValue()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setId(1L);
        limit.setAttentionValue(10.0);

        Long originalId = limit.getId();

        // When
        limit.setAttentionValue(15.0);
        limit.setAlertValue(25.0);
        limit.setEmergencyValue(35.0);

        // Then
        assertThat(limit.getId()).isEqualTo(originalId);
        assertThat(limit.getAttentionValue()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Should support limits with equal threshold values")
    void shouldSupportLimitsWithEqualThresholdValues() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(20.0);
        limit.setAlertValue(20.0);
        limit.setEmergencyValue(20.0);

        // Then
        assertThat(limit.getAttentionValue())
                .isEqualTo(limit.getAlertValue())
                .isEqualTo(limit.getEmergencyValue());
    }

    @Test
    @DisplayName("Should handle descending threshold values")
    void shouldHandleDescendingThresholdValues() {
        // Given - For some instruments, lower values might be more dangerous
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(30.0);
        limit.setAlertValue(20.0);
        limit.setEmergencyValue(10.0);

        // Then
        assertThat(limit.getAttentionValue()).isGreaterThan(limit.getAlertValue());
        assertThat(limit.getAlertValue()).isGreaterThan(limit.getEmergencyValue());
    }

    @Test
    @DisplayName("Should handle scientific notation values")
    void shouldHandleScientificNotationValues() {
        // Given
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setAttentionValue(1.5e-5);
        limit.setAlertValue(2.5e-5);
        limit.setEmergencyValue(3.5e-5);

        // Then
        assertThat(limit.getAttentionValue()).isEqualTo(1.5e-5);
        assertThat(limit.getAlertValue()).isEqualTo(2.5e-5);
        assertThat(limit.getEmergencyValue()).isEqualTo(3.5e-5);
    }
}
