package com.geosegbar.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.geosegbar.exceptions.InvalidInputException;

/**
 * Regras de preenchimento de checklist — espelho servidor da especificação
 * "Regras de Preenchimento de Checklist — Mobile → Web".
 *
 * São duas regras distintas, aplicadas em sequência:
 *
 * 1. {@link #validateTransition} — qual opção pode ser escolhida, dada a
 * resposta da inspeção ANTERIOR daquele mesmo ponto (seção 2 do documento).
 *
 * 2. {@link #validateAnswerFields} — quais campos a opção escolhida torna
 * obrigatórios (seção 3 do documento).
 *
 * A ordem importa: uma transição inválida é erro mais fundamental que um campo
 * faltando, e é o que o inspetor precisa ver primeiro.
 */
public final class ChecklistOptionTransitionValidator {

    private static final Set<String> ALLOWED_WHEN_NO_PREVIOUS = Set.of("PV", "NI", "NE");
    private static final Set<String> BLOCKED_AFTER_NE = Set.of("PC", "AU", "DM", "DS");
    private static final Set<String> BLOCKED_AFTER_PV = Set.of("PV", "NE");
    private static final Set<String> BLOCKED_AFTER_AU_PC_DM = Set.of("NE", "PV");
    private static final Set<String> BLOCKED_AFTER_DS = Set.of("PC", "AU", "DM", "DS");

    /**
     * Evolução de uma anomalia já registrada: exigem observação e foto.
     */
    private static final Set<String> EVOLUTION_LABELS = Set.of("AU", "DM", "PC", "DS");

    private ChecklistOptionTransitionValidator() {
    }

    public static void validateTransition(String previousLabel, String newLabel, String questionText) {
        if (previousLabel == null) {
            if (!ALLOWED_WHEN_NO_PREVIOUS.contains(newLabel)) {
                throw new InvalidInputException(
                        "Opção '" + newLabel + "' não é permitida para a pergunta '" + questionText
                        + "' pois não há resposta anterior. Opções permitidas: " + ALLOWED_WHEN_NO_PREVIOUS);
            }
            return;
        }

        Set<String> blocked = switch (previousLabel) {
            case "NE" ->
                BLOCKED_AFTER_NE;
            case "PV" ->
                BLOCKED_AFTER_PV;
            case "AU", "PC", "DM" ->
                BLOCKED_AFTER_AU_PC_DM;
            case "DS" ->
                BLOCKED_AFTER_DS;
            default ->
                Set.of();
        };

        if (blocked.contains(newLabel)) {
            throw new InvalidInputException(
                    "Opção '" + newLabel + "' não é permitida para a pergunta '" + questionText
                    + "' pois a resposta da inspeção anterior foi '" + previousLabel + "'.");
        }
    }

    /**
     * Campos obrigatórios por opção, na submissão de um checklist novo.
     *
     * <pre>
     * NE              — nada
     * NI              — observação
     * AU / DM / PC / DS — observação + foto
     * PV              — observação + foto + localização + recomendação + nível + status
     * </pre>
     *
     * Recomendação, nível de perigo e status do PV chegam preenchidos por padrão
     * do app ('--' / '--'), mas continuam obrigatórios: um cliente que os
     * enviar vazios está mandando uma anomalia sem classificação nenhuma.
     */
    public static void validateAnswerFields(
            String optionLabel,
            String comment,
            boolean hasPhotos,
            boolean hasLocation,
            String recommendation,
            Long dangerLevelId,
            Long statusId,
            boolean requireObservationOnNi,
            String questionText) {

        List<String> missing = collectMissing(
                optionLabel, comment, hasPhotos, hasLocation, requireObservationOnNi);

        if ("PV".equals(optionLabel)) {
            if (recommendation == null || recommendation.isBlank()) {
                missing.add("Recomendação");
            }
            if (dangerLevelId == null) {
                missing.add("Nível de Perigo");
            }
            if (statusId == null) {
                missing.add("Status");
            }
        }

        throwIfMissing(optionLabel, questionText, missing);
    }

    /**
     * Mesma tabela da submissão, menos recomendação/nível/status.
     *
     * A edição de uma resposta já gravada (PUT de correção pela web) troca a
     * opção, o comentário e as fotos — os três campos de classificação da
     * anomalia não trafegam nesse payload, então não há o que validar aqui.
     */
    public static void validateEditedAnswerFields(
            String optionLabel,
            String comment,
            boolean hasPhotos,
            boolean hasLocation,
            boolean requireObservationOnNi,
            String questionText) {

        throwIfMissing(optionLabel, questionText,
                collectMissing(optionLabel, comment, hasPhotos, hasLocation, requireObservationOnNi));
    }

    private static List<String> collectMissing(
            String optionLabel, String comment, boolean hasPhotos, boolean hasLocation,
            boolean requireObservationOnNi) {

        List<String> missing = new ArrayList<>();

        if ("NE".equals(optionLabel)) {
            return missing;
        }

        boolean isPV = "PV".equals(optionLabel);
        boolean isEvolution = EVOLUTION_LABELS.contains(optionLabel);
        boolean isNI = "NI".equals(optionLabel);

        if (!isPV && !isEvolution && !isNI) {
            return missing;
        }

        // NI é o único ponto em que servidor e app divergem de propósito.
        //
        // O app exige observação no NI; este servidor nunca exigiu, e 174 das
        // 236 respostas NI já gravadas em produção não têm observação — ligar a
        // regra sem aviso passaria a recusar a maior parte dos "não
        // inspecionado" que chegam hoje. Fica atrás de
        // `checklist.validation.require-observation-on-ni`, para ser ligada
        // quando as duas pontas estiverem alinhadas.
        boolean exigeObservacao = isPV || isEvolution || requireObservationOnNi;

        if (exigeObservacao && (comment == null || comment.isBlank())) {
            missing.add("Observação");
        }

        if ((isPV || isEvolution) && !hasPhotos) {
            missing.add("Foto");
        }

        if (isPV && !hasLocation) {
            missing.add("Localização");
        }

        return missing;
    }

    private static void throwIfMissing(String optionLabel, String questionText, List<String> missing) {
        if (missing.isEmpty()) {
            return;
        }

        throw new InvalidInputException(
                "A resposta '" + optionLabel + "' para a pergunta '" + questionText
                + "' exige obrigatoriamente: " + humanJoin(missing) + ".");
    }

    private static String humanJoin(List<String> items) {
        if (items.size() == 1) {
            return items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1))
                + " e " + items.get(items.size() - 1);
    }
}
