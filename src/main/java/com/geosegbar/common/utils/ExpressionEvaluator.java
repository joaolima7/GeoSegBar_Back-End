package com.geosegbar.common.utils;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Avalia equações de outputs usando SpEL.
 * <p>
 * Pipeline (idêntico em avaliação e validação):
 * <ol>
 *   <li>{@code normalizeMathFunctions}: remove o prefixo {@code Math.} e prefixa
 *       funções suportadas com {@code #} (ex.: {@code sqrt(} → {@code #sqrt(}),
 *       de forma case-insensitive.</li>
 *   <li>{@code substituteVariables}: prefixa os acrônimos de inputs/constantes
 *       com {@code #} (ex.: {@code BASE} → {@code #BASE}).</li>
 * </ol>
 * É essencial que validação e avaliação usem o MESMO pipeline — caso contrário a
 * equação passa na validação mas falha na hora de calcular (ou vice-versa).
 */
public class ExpressionEvaluator {

    private static final ExpressionParser parser = new SpelExpressionParser();
    private static final Pattern MATH_PREFIX_PATTERN = Pattern.compile("\\bMath\\s*\\.\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * Mapa nome-da-função (em minúsculas) → assinatura de parâmetros, usado para
     * registrar os métodos de {@link Math} no contexto SpEL.
     */
    private static final Map<String, Class<?>[]> MATH_FUNCTIONS = new HashMap<>();

    static {
        // Trigonométricas
        MATH_FUNCTIONS.put("sin", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("cos", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("tan", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("asin", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("acos", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("atan", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("sinh", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("cosh", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("tanh", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("toradians", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("todegrees", new Class<?>[]{double.class});
        // Exponenciais / logaritmos
        MATH_FUNCTIONS.put("exp", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("log", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("log10", new Class<?>[]{double.class});
        // Raízes / valor absoluto / sinal
        MATH_FUNCTIONS.put("sqrt", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("cbrt", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("abs", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("signum", new Class<?>[]{double.class});
        // Arredondamentos
        MATH_FUNCTIONS.put("floor", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("ceil", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("rint", new Class<?>[]{double.class});
        MATH_FUNCTIONS.put("round", new Class<?>[]{double.class}); // Math.round(double) -> long
        // Duas variáveis
        MATH_FUNCTIONS.put("pow", new Class<?>[]{double.class, double.class});
        MATH_FUNCTIONS.put("atan2", new Class<?>[]{double.class, double.class});
        MATH_FUNCTIONS.put("hypot", new Class<?>[]{double.class, double.class});
        MATH_FUNCTIONS.put("min", new Class<?>[]{double.class, double.class});
        MATH_FUNCTIONS.put("max", new Class<?>[]{double.class, double.class});
    }

    /**
     * Padrão que casa qualquer nome de função suportada seguido de "(",
     * insensível a maiúsculas/minúsculas e que não faça parte de um identificador
     * maior (ex.: não casa "mysqrt(").
     */
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(
            "(?<![#A-Za-z0-9_.])(" + MATH_FUNCTIONS.keySet().stream()
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"))
            + ")\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    private static StandardEvaluationContext buildContext(Map<String, Double> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (Map.Entry<String, Double> entry : variables.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        registerMathFunctions(context);
        return context;
    }

    private static void registerMathFunctions(StandardEvaluationContext context) {
        for (Map.Entry<String, Class<?>[]> entry : MATH_FUNCTIONS.entrySet()) {
            try {
                // O nome do método em Math respeita o case real (ex.: log10, atan2,
                // toRadians). Resolve-se a partir do mapa case-insensitive.
                Method method = resolveMathMethod(entry.getKey(), entry.getValue());
                if (method != null) {
                    // Registra com o nome em minúsculas (forma usada após a normalização).
                    context.registerFunction(entry.getKey(), method);
                }
            } catch (Exception ignored) {
                // método padrão de Math sempre existe; ignora se algo der errado
            }
        }
    }

    /**
     * Resolve o método de {@link Math} a partir do nome em minúsculas, mapeando
     * para o nome real (camelCase) quando necessário.
     */
    private static Method resolveMathMethod(String lowerName, Class<?>[] paramTypes) throws NoSuchMethodException {
        String realName = switch (lowerName) {
            case "log10" ->
                "log10";
            case "atan2" ->
                "atan2";
            case "toradians" ->
                "toRadians";
            case "todegrees" ->
                "toDegrees";
            default ->
                lowerName;
        };
        return Math.class.getMethod(realName, paramTypes);
    }

    /**
     * Normaliza as funções matemáticas: remove o prefixo {@code Math.} e prefixa
     * o nome da função (em minúsculas) com {@code #}, mantendo o parêntese.
     */
    private static String normalizeMathFunctions(String expression) {
        String processed = MATH_PREFIX_PATTERN.matcher(expression).replaceAll("");
        Matcher m = FUNCTION_CALL_PATTERN.matcher(processed);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            // group(1) = nome da função como digitado; normaliza para minúsculas
            String replacement = "#" + m.group(1).toLowerCase() + "(";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return processed.isEmpty() ? processed : sb.toString();
    }

    /**
     * Substitui os acrônimos de variáveis (inputs/constantes) por {@code #nome}.
     * As funções já devem ter sido normalizadas antes (para não colidir).
     */
    private static String substituteVariables(String expression, Set<String> variableNames) {
        String processed = expression;
        for (String varName : variableNames.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList()) {
            processed = processed.replaceAll(
                    "(?<![#A-Za-z0-9_])" + Pattern.quote(varName) + "(?![A-Za-z0-9_])",
                    Matcher.quoteReplacement("#" + varName));
        }
        return processed;
    }

    /**
     * Avalia a equação com os valores fornecidos.
     */
    public static Double evaluate(String expression, Map<String, Double> variables) {
        StandardEvaluationContext context = buildContext(variables);
        String processed = normalizeMathFunctions(expression);
        processed = substituteVariables(processed, variables.keySet());
        Expression exp = parser.parseExpression(processed);
        return exp.getValue(context, Double.class);
    }

    /**
     * Valida a sintaxe da equação usando o MESMO pipeline da avaliação. Atribui
     * o valor 1.0 a cada variável conhecida e tenta avaliar — se a equação tiver
     * função não suportada ou sintaxe inválida, lança exceção.
     *
     * @param expression equação (pode conter espaços; serão tratados pelo SpEL)
     * @param variableNames acrônimos de inputs/constantes presentes na equação
     */
    public static void validateSyntax(String expression, Set<String> variableNames) throws Exception {
        Map<String, Double> dummy = new HashMap<>();
        for (String name : variableNames) {
            dummy.put(name, 1.0);
        }
        // Reaproveita exatamente o caminho de avaliação.
        evaluate(expression, dummy);
    }

    /**
     * Detecta nomes de função (identificador seguido de "(") que NÃO são
     * funções matemáticas suportadas — útil para mensagens de erro claras.
     */
    public static Set<String> findUnsupportedFunctions(String expression) {
        Set<String> unsupported = new LinkedHashSet<>();
        if (expression == null) {
            return unsupported;
        }
        String withoutMathPrefix = MATH_PREFIX_PATTERN.matcher(expression).replaceAll("");
        Matcher m = Pattern.compile("(?<![#A-Za-z0-9_.])([A-Za-z][A-Za-z0-9_]*)\\s*\\(").matcher(withoutMathPrefix);
        while (m.find()) {
            String name = m.group(1);
            if (!MATH_FUNCTIONS.containsKey(name.toLowerCase())) {
                unsupported.add(name);
            }
        }
        return unsupported;
    }

    public static boolean isMathFunction(String name) {
        return name != null && MATH_FUNCTIONS.containsKey(name.toLowerCase());
    }

    public static String friendlySyntaxErrorMessage(String expression) {
        Set<String> unsupported = findUnsupportedFunctions(expression);
        if (!unsupported.isEmpty()) {
            return "Equação inválida: a(s) função(ões) " + String.join(", ", unsupported)
                    + " não é(são) suportada(s). Funções disponíveis: "
                    + supportedFunctionsList() + ". "
                    + "Para potência use pow(BASE, 2), para raiz quadrada sqrt(BASE), "
                    + "e exemplos como abs(X), min(A, B), max(A, B).";
        }
        return "Equação inválida. Use os acrônimos dos inputs/constantes, operadores +, -, *, / e parênteses. "
                + "Funções disponíveis: " + supportedFunctionsList() + ". "
                + "Exemplos: pow(BASE, 2), sqrt(BASE), abs(BASE), min(A, B), max(A, B).";
    }

    private static String supportedFunctionsList() {
        return MATH_FUNCTIONS.keySet().stream()
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
