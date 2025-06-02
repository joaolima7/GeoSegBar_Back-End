package com.geosegbar.infra.math;

import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class ExpressionEvaluator {

    private static final ExpressionParser parser = new SpelExpressionParser();

    public static Double evaluate(String expression, Map<String, Double> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        for (Map.Entry<String, Double> entry : variables.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        String processedExpression = expression;
        for (String varName : variables.keySet()) {
            processedExpression = processedExpression.replaceAll("\\b" + varName + "\\b", "#" + varName);
        }

        Expression exp = parser.parseExpression(processedExpression);
        return exp.getValue(context, Double.class);
    }

    public static void validateSyntax(String expression) throws Exception {
        String testExpression = expression.replaceAll("[A-Za-z][A-Za-z0-9_]*", "1.0");
        parser.parseExpression(testExpression);
    }
}
