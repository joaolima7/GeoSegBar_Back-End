package com.geosegbar.unit.infra.checklist_response.services;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.infra.checklist_response.services.InspectionDateDivergence;
import com.geosegbar.infra.checklist_response.services.InspectionDateDivergence.AnswerSnapshot;

/**
 * Mudar a data de uma inspecao muda a ordem da linha do tempo, e com isso muda
 * qual e a inspecao anterior de cada ponto. Uma resposta ja gravada pode passar
 * a ser uma transicao que o sistema recusaria hoje.
 */
@DisplayName("InspectionDateDivergence")
class InspectionDateDivergenceTest extends BaseUnitTest {

    private static final LocalDateTime ABRIL = LocalDateTime.of(2026, 4, 10, 9, 0);
    private static final LocalDateTime MAIO = LocalDateTime.of(2026, 5, 10, 9, 0);
    private static final LocalDateTime JUNHO = LocalDateTime.of(2026, 6, 10, 9, 0);

    private static AnswerSnapshot snap(Long responseId, LocalDateTime date, String label) {
        return new AnswerSnapshot(responseId, date, 10L, 1L, "Canaletas obstruídas", label);
    }

    @Test
    @DisplayName("sem mudar nada de lugar, nao ha divergencia")
    void noDivergenceWhenOrderIsUnchanged() {
        List<AnswerSnapshot> cadeia = List.of(
                snap(1L, ABRIL, "NE"),
                snap(2L, MAIO, "PV"));

        var divergencias = InspectionDateDivergence.compute(cadeia, 1L, ABRIL.plusDays(1));

        assertThat(divergencias).isEmpty();
    }

    @Test
    @DisplayName("mover uma inspecao para depois de outra invalida a que passou a ser posterior")
    void reorderingBreaksTheLaterAnswer() {
        // Hoje: abril NE -> maio PV (valido, PV depois de NE e permitido).
        // Movendo abril para depois de maio: maio passa a nao ter anterior, e PV
        // sem anterior e permitido; mas abril NE passa a vir depois de PV, e NE
        // depois de PV e bloqueado.
        List<AnswerSnapshot> cadeia = List.of(
                snap(1L, ABRIL, "NE"),
                snap(2L, MAIO, "PV"));

        var divergencias = InspectionDateDivergence.compute(cadeia, 1L, JUNHO);

        assertThat(divergencias).hasSize(1);
        assertThat(divergencias.get(0).checklistResponseId()).isEqualTo(1L);
        assertThat(divergencias.get(0).label()).isEqualTo("NE");
        assertThat(divergencias.get(0).previousLabelAfter()).isEqualTo("PV");
    }

    @Test
    @DisplayName("nao reporta divergencia que ja existia antes da mudanca")
    void ignoresPreExistingViolations() {
        // PC sem anterior ja e invalido hoje; mover outra inspecao nao torna isso
        // novidade, e o usuario nao deve ser cobrado por algo que ja estava assim.
        List<AnswerSnapshot> cadeia = List.of(
                snap(1L, ABRIL, "PC"),
                snap(2L, MAIO, "AU"));

        var divergencias = InspectionDateDivergence.compute(cadeia, 2L, MAIO.plusDays(1));

        assertThat(divergencias).isEmpty();
    }

    @Test
    @DisplayName("perguntas diferentes sao cadeias independentes")
    void questionsAreIndependentChains() {
        List<AnswerSnapshot> cadeia = List.of(
                new AnswerSnapshot(1L, ABRIL, 10L, 1L, "P1", "NE"),
                new AnswerSnapshot(2L, MAIO, 10L, 1L, "P1", "PV"),
                new AnswerSnapshot(1L, ABRIL, 10L, 2L, "P2", "NE"),
                new AnswerSnapshot(2L, MAIO, 10L, 2L, "P2", "NE"));

        var divergencias = InspectionDateDivergence.compute(cadeia, 1L, JUNHO);

        assertThat(divergencias).extracting(InspectionDateDivergence.Divergence::questionId)
                .containsOnly(1L);
    }

    @Test
    @DisplayName("a mesma pergunta em questionarios diferentes nao se mistura")
    void questionnairesAreIndependentChains() {
        List<AnswerSnapshot> cadeia = List.of(
                new AnswerSnapshot(1L, ABRIL, 10L, 1L, "P1", "NE"),
                new AnswerSnapshot(2L, MAIO, 20L, 1L, "P1", "PV"));

        var divergencias = InspectionDateDivergence.compute(cadeia, 1L, JUNHO);

        assertThat(divergencias).isEmpty();
    }

    @Test
    @DisplayName("cadeia vazia nao explode")
    void toleratesEmptyChain() {
        assertThat(InspectionDateDivergence.compute(List.of(), 1L, ABRIL)).isEmpty();
    }
}
