package com.geosegbar.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.geosegbar.exceptions.InvalidInputException;

public final class ChecklistOptionTransitionValidator {

    private static final Set<String> ALLOWED_WHEN_NO_PREVIOUS = Set.of("PV", "NI", "NE");
    private static final Set<String> BLOCKED_AFTER_NE = Set.of("PC", "AU", "DM", "DS");
    private static final Set<String> BLOCKED_AFTER_PV = Set.of("PV", "NE");
    private static final Set<String> BLOCKED_AFTER_AU_PC_DM = Set.of("NE", "PV");
    private static final Set<String> BLOCKED_AFTER_DS = Set.of("PC", "AU", "DM", "DS");

    private static final Set<String> EVIDENCE_REQUIRED_LABELS = Set.of("PV", "AU", "DM", "PC", "DS");

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
            case "NE" -> BLOCKED_AFTER_NE;
            case "PV" -> BLOCKED_AFTER_PV;
            case "AU", "PC", "DM" -> BLOCKED_AFTER_AU_PC_DM;
            case "DS" -> BLOCKED_AFTER_DS;
            default -> Set.of();
        };

        if (blocked.contains(newLabel)) {
            throw new InvalidInputException(
                    "Opção '" + newLabel + "' não é permitida para a pergunta '" + questionText
                            + "' pois a resposta da inspeção anterior foi '" + previousLabel + "'.");
        }
    }

    public static void validateEvidence(String optionLabel, String comment, boolean hasPhotos, String questionText) {
        if (!EVIDENCE_REQUIRED_LABELS.contains(optionLabel)) {
            return;
        }

        List<String> missing = new ArrayList<>();
        if (comment == null || comment.isBlank()) {
            missing.add("Observação/Comentário");
        }
        if (!hasPhotos) {
            missing.add("Foto");
        }

        if (!missing.isEmpty()) {
            throw new InvalidInputException(
                    "A resposta '" + optionLabel + "' para a pergunta '" + questionText
                            + "' exige obrigatoriamente: " + String.join(" e ", missing) + ".");
        }
    }
}
