package com.geosegbar.common.utils;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class ExpressionEvaluator {

    private static final ExpressionParser parser = new SpelExpressionParser();

    private static final String[] MATH_FUNCTION_NAMES = {
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh",
        "exp", "log", "log10",
        "sqrt", "cbrt", "abs",
        "floor", "ceil", "round",
        "pow", "atan2", "min", "max"
    };

    private static final Class<?>[][] MATH_FUNCTION_PARAM_TYPES = {
        {double.class}, {double.class}, {double.class},   // sin, cos, tan
        {double.class}, {double.class}, {double.class},   // asin, acos, atan
        {double.class}, {double.class}, {double.class},   // sinh, cosh, tanh
        {double.class}, {double.class}, {double.class},   // exp, log, log10
        {double.class}, {double.class}, {double.class},   // sqrt, cbrt, abs
        {double.class}, {double.class}, {double.class},   // floor, ceil, round
        {double.class, double.class},                      // pow
        {double.class, double.class},                      // atan2
        {double.class, double.class},                      // min
        {double.class, double.class}                       // max
    };

    private static StandardEvaluationContext buildContext(Map<String, Double> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        for (Map.Entry<String, Double> entry : variables.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        registerMathFunctions(context);

        return context;
    }

    private static void registerMathFunctions(StandardEvaluationContext context) {
        for (int i = 0; i < MATH_FUNCTION_NAMES.length; i++) {
            String name = MATH_FUNCTION_NAMES[i];
            try {
                Method method = Math.class.getMethod(name, MATH_FUNCTION_PARAM_TYPES[i]);
                context.registerFunction(name, method);
            } catch (NoSuchMethodException e) {
                // skip if not found — should not happen for standard Math methods
            }
        }
    }

    private static String substituteVariables(String expression, Map<String, Double> variables) {
        String processed = expression;
        for (String varName : variables.keySet()) {
            processed = processed.replaceAll("\\b" + varName + "\\b", "#" + varName);
        }
        return processed;
    }

    public static Double evaluate(String expression, Map<String, Double> variables) {
        StandardEvaluationContext context = buildContext(variables);
        String processedExpression = substituteVariables(expression, variables);
        Expression exp = parser.parseExpression(processedExpression);
        return exp.getValue(context, Double.class);
    }

    public static void validateSyntax(String expression) throws Exception {
        StandardEvaluationContext context = new StandardEvaluationContext();
        registerMathFunctions(context);

        // Replace variable-like identifiers (not registered math functions) with a dummy variable
        context.setVariable("__v__", 1.0);
        String testExpression = expression.replaceAll("[A-Za-z][A-Za-z0-9_]*", "#__v__");

        Expression exp = parser.parseExpression(testExpression);
        exp.getValue(context, Double.class);
    }
}
