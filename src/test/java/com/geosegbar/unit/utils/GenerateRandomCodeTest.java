package com.geosegbar.unit.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.utils.GenerateRandomCode;
import com.geosegbar.config.BaseUnitTest;

@Tag("unit")
class GenerateRandomCodeTest extends BaseUnitTest {

    @Test
    @DisplayName("Should generate 6 digit code")
    void shouldGenerate6DigitCode() {
        // When
        String code = GenerateRandomCode.generateRandomCode();

        // Then
        assertThat(code).hasSize(6);
    }

    @Test
    @DisplayName("Should generate numeric code matching pattern")
    void shouldGenerateNumericCodeMatchingPattern() {
        // When
        String code = GenerateRandomCode.generateRandomCode();

        // Then
        assertThat(code).matches("\\d{6}");
    }

    @Test
    @DisplayName("Should generate non null code")
    void shouldGenerateNonNullCode() {
        // When
        String code = GenerateRandomCode.generateRandomCode();

        // Then
        assertThat(code).isNotNull();
    }

    @Test
    @DisplayName("Should generate non empty code")
    void shouldGenerateNonEmptyCode() {
        // When
        String code = GenerateRandomCode.generateRandomCode();

        // Then
        assertThat(code).isNotEmpty();
    }

    @Test
    @DisplayName("Should generate code within valid range 100000-999999")
    void shouldGenerateCodeWithinValidRange() {
        // When
        String code = GenerateRandomCode.generateRandomCode();
        int codeValue = Integer.parseInt(code);

        // Then
        assertThat(codeValue).isBetween(100000, 999999);
    }

    @Test
    @DisplayName("Should generate different codes in multiple calls")
    void shouldGenerateDifferentCodesInMultipleCalls() {
        // Given
        Set<String> codes = new HashSet<>();
        int iterations = 100;

        // When
        for (int i = 0; i < iterations; i++) {
            codes.add(GenerateRandomCode.generateRandomCode());
        }

        // Then
        assertThat(codes.size()).isGreaterThan(iterations / 2); // At least 50% unique
    }

    @Test
    @DisplayName("Should generate codes starting with non-zero digit")
    void shouldGenerateCodesStartingWithNonZeroDigit() {
        // When
        String code = GenerateRandomCode.generateRandomCode();
        char firstChar = code.charAt(0);

        // Then
        assertThat(firstChar).isNotEqualTo('0');
        assertThat(firstChar).isBetween('1', '9');
    }

    @Test
    @DisplayName("Should generate codes with all digits being numeric")
    void shouldGenerateCodesWithAllDigitsBeingNumeric() {
        // When
        String code = GenerateRandomCode.generateRandomCode();

        // Then
        for (char c : code.toCharArray()) {
            assertThat(c).isBetween('0', '9');
        }
    }

    @Test
    @DisplayName("Should generate multiple consecutive codes that are distinct")
    void shouldGenerateMultipleConsecutiveCodesThatAreDistinct() {
        // When
        String code1 = GenerateRandomCode.generateRandomCode();
        String code2 = GenerateRandomCode.generateRandomCode();
        String code3 = GenerateRandomCode.generateRandomCode();

        // Then
        assertThat(code1).isNotEqualTo(code2);
        assertThat(code2).isNotEqualTo(code3);
        assertThat(code1).isNotEqualTo(code3);
    }

    @Test
    @DisplayName("Should generate code as string type")
    void shouldGenerateCodeAsStringType() {
        // When
        String code = GenerateRandomCode.generateRandomCode();

        // Then
        assertThat(code).isInstanceOf(String.class);
    }
}
