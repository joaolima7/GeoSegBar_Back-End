package com.geosegbar.infra.checklist_response.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.geosegbar.common.utils.ChecklistOptionTransitionValidator;

/**
 * Mudar a data de uma inspecao a move na linha do tempo, e com isso muda qual e
 * a inspecao anterior de cada ponto — logo, quais marcacoes sao permitidas. Uma
 * resposta JA GRAVADA, de outra inspecao, pode passar a ser uma transicao que o
 * sistema recusaria hoje.
 *
 * <p>Esta classe calcula, antes de gravar, exatamente quais respostas passariam
 * a estar irregulares. Reporta apenas o que a mudanca CRIA: o que ja estava
 * irregular antes nao e novidade e nao deve ser cobrado de quem mexeu na data.
 */
public final class InspectionDateDivergence {

    private InspectionDateDivergence() {
    }

    public record AnswerSnapshot(
            Long checklistResponseId,
            LocalDateTime inspectionDate,
            Long templateQuestionnaireId,
            Long questionId,
            String questionText,
            String label) {

    }

    public record Divergence(
            Long checklistResponseId,
            LocalDateTime inspectionDate,
            Long templateQuestionnaireId,
            Long questionId,
            String questionText,
            String label,
            String previousLabelBefore,
            String previousLabelAfter,
            String motivo) {

    }

    public static List<Divergence> compute(List<AnswerSnapshot> chain,
            Long movedResponseId, LocalDateTime newDate) {

        Map<ChainKey, String> antes = violations(chain, null, null);
        Map<ChainKey, String> depois = violations(chain, movedResponseId, newDate);

        List<Divergence> divergencias = new ArrayList<>();
        for (Map.Entry<ChainKey, String> entry : depois.entrySet()) {
            if (antes.containsKey(entry.getKey())) {
                continue;
            }
            ChainKey key = entry.getKey();
            AnswerSnapshot snapshot = key.snapshot();
            divergencias.add(new Divergence(
                    snapshot.checklistResponseId(),
                    dateOf(snapshot, movedResponseId, newDate),
                    snapshot.templateQuestionnaireId(),
                    snapshot.questionId(),
                    snapshot.questionText(),
                    snapshot.label(),
                    previousIn(chain, null, null, key),
                    previousIn(chain, movedResponseId, newDate, key),
                    entry.getValue()));
        }

        divergencias.sort(Comparator
                .comparing(Divergence::inspectionDate)
                .thenComparing(Divergence::questionId));
        return divergencias;
    }

    /**
     * Percorre cada cadeia (questionario + pergunta) na ordem das datas e reusa a
     * MESMA validacao da gravacao, para previa e gravacao nunca discordarem.
     */
    private static Map<ChainKey, String> violations(List<AnswerSnapshot> chain,
            Long movedResponseId, LocalDateTime newDate) {

        Map<ChainKey, String> found = new HashMap<>();

        for (List<AnswerSnapshot> ordered : groupAndOrder(chain, movedResponseId, newDate)) {
            String previous = null;
            for (AnswerSnapshot snapshot : ordered) {
                try {
                    ChecklistOptionTransitionValidator.validateTransition(
                            previous, snapshot.label(), snapshot.questionText());
                } catch (RuntimeException e) {
                    found.put(new ChainKey(snapshot), e.getMessage());
                }
                previous = snapshot.label();
            }
        }

        return found;
    }

    private static String previousIn(List<AnswerSnapshot> chain,
            Long movedResponseId, LocalDateTime newDate, ChainKey key) {

        for (List<AnswerSnapshot> ordered : groupAndOrder(chain, movedResponseId, newDate)) {
            String previous = null;
            for (AnswerSnapshot snapshot : ordered) {
                if (new ChainKey(snapshot).equals(key)) {
                    return previous;
                }
                previous = snapshot.label();
            }
        }
        return null;
    }

    private static List<List<AnswerSnapshot>> groupAndOrder(List<AnswerSnapshot> chain,
            Long movedResponseId, LocalDateTime newDate) {

        Map<String, List<AnswerSnapshot>> grouped = new HashMap<>();
        for (AnswerSnapshot snapshot : chain) {
            grouped.computeIfAbsent(
                    snapshot.templateQuestionnaireId() + ":" + snapshot.questionId(),
                    k -> new ArrayList<>()).add(snapshot);
        }

        List<List<AnswerSnapshot>> result = new ArrayList<>();
        for (List<AnswerSnapshot> group : grouped.values()) {
            List<AnswerSnapshot> ordered = new ArrayList<>(group);
            ordered.sort(Comparator
                    .comparing((AnswerSnapshot s) -> dateOf(s, movedResponseId, newDate))
                    .thenComparing(AnswerSnapshot::checklistResponseId));
            result.add(ordered);
        }
        return result;
    }

    private static LocalDateTime dateOf(AnswerSnapshot snapshot,
            Long movedResponseId, LocalDateTime newDate) {
        if (movedResponseId != null && newDate != null
                && Objects.equals(snapshot.checklistResponseId(), movedResponseId)) {
            return newDate;
        }
        return snapshot.inspectionDate();
    }

    private record ChainKey(Long checklistResponseId, Long templateQuestionnaireId, Long questionId,
            AnswerSnapshot snapshot) {

        ChainKey(AnswerSnapshot snapshot) {
            this(snapshot.checklistResponseId(), snapshot.templateQuestionnaireId(),
                    snapshot.questionId(), snapshot);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ChainKey key)) {
                return false;
            }
            return Objects.equals(checklistResponseId, key.checklistResponseId)
                    && Objects.equals(templateQuestionnaireId, key.templateQuestionnaireId)
                    && Objects.equals(questionId, key.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(checklistResponseId, templateQuestionnaireId, questionId);
        }
    }
}
