package com.geosegbar.unit.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.utils.ExpressionEvaluator;
import com.geosegbar.config.BaseUnitTest;

@Tag("unit")
class ExpressionEvaluatorTest extends BaseUnitTest {

    @Test
    @DisplayName("Should evaluate simple addition expression")
    void shouldEvaluateSimpleAdditionExpression() {
        // Given
        String expression = "x + y";
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 10.0);
        variables.put("y", 5.0);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Should evaluate simple subtraction expression")
    void shouldEvaluateSimpleSubtractionExpression() {
        // Given
        String expression = "x - y";
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 10.0);
        variables.put("y", 3.0);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isEqualTo(7.0);
    }

    @Test
    @DisplayName("Should evaluate simple multiplication expression")
    void shouldEvaluateSimpleMultiplicationExpression() {
        // Given
        String expression = "x * y";
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 4.0);
        variables.put("y", 5.0);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Should evaluate simple division expression")
    void shouldEvaluateSimpleDivisionExpression() {
        // Given
        String expression = "x / y";
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 20.0);
        variables.put("y", 4.0);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should evaluate complex expression with multiple variables")
    void shouldEvaluateComplexExpressionWithMultipleVariables() {
        // Given
        String expression = "(x * 2) + (y / 3) - z";
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 10.0);
        variables.put("y", 9.0);
        variables.put("z", 2.0);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isEqualTo(21.0); // (10*2) + (9/3) - 2 = 20 + 3 - 2 = 21
    }

    @Test
    @DisplayName("Should evaluate expression with parentheses for precedence")
    void shouldEvaluateExpressionWithParenthesesForPrecedence() {
        // Given
        String expression = "(x + y) * z";
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 3.0);
        variables.put("y", 2.0);
        variables.put("z", 4.0);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isEqualTo(20.0); // (3+2)*4 = 5*4 = 20
    }

    @Test
    @DisplayName("Should evaluate expression with variables containing underscore")
    void shouldEvaluateExpressionWithVariablesContainingUnderscore() {
        // Given
        String expression = "X_1 + Y_2";
        Map<String, Double> variables = new HashMap<>();
        variables.put("X_1", 15.0);
        variables.put("Y_2", 25.0);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isEqualTo(40.0);
    }

    @Test
    @DisplayName("Should evaluate expression with decimal values")
    void shouldEvaluateExpressionWithDecimalValues() {
        // Given
        String expression = "x + y";
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 10.5);
        variables.put("y", 5.3);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isCloseTo(15.8, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("Should evaluate expression with negative values")
    void shouldEvaluateExpressionWithNegativeValues() {
        // Given
        String expression = "x + y";
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", -10.0);
        variables.put("y", 5.0);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isEqualTo(-5.0);
    }

    @Test
    @DisplayName("Should validate syntax for valid expression")
    void shouldValidateSyntaxForValidExpression() throws Exception {
        // Given
        String expression = "x + y * z";

        // When & Then (no exception thrown)
        ExpressionEvaluator.validateSyntax(expression);
    }

    @Test
    @DisplayName("Should throw exception for invalid syntax")
    void shouldThrowExceptionForInvalidSyntax() {
        // Given
        String expression = "(x +"; // invalid syntax (unclosed parenthesis)

        // When & Then
        assertThatThrownBy(() -> ExpressionEvaluator.validateSyntax(expression))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should evaluate expression with single variable")
    void shouldEvaluateExpressionWithSingleVariable() {
        // Given
        String expression = "x * 2";
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 7.0);

        // When
        Double result = ExpressionEvaluator.evaluate(expression, variables);

        // Then
        assertThat(result).isEqualTo(14.0);
    }
}
