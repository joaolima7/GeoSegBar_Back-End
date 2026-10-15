package com.geosegbar.infra.checklist_response.dtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * O que o app precisa para editar uma inspeção já enviada na mesma tela do
 * preenchimento: a resposta completa e, por pergunta, a opção da inspeção
 * anterior na data de referência — a mesma consulta que a gravação usa para
 * validar a transição, então o app e o servidor nunca divergem.
 *
 * @param referenceDate a data pedida, ou a data da própria inspeção.
 * @param previousLabels uma linha por (questionário modelo, pergunta) com
 * resposta anterior; NI é pulada, como na validação.
 */
public record ChecklistResponseEditContextDTO(
        ChecklistResponseDetailDTO response,
        LocalDateTime referenceDate,
        List<PreviousLabel> previousLabels) {

    public record PreviousLabel(Long templateQuestionnaireId, Long questionId, String label) {

    }
}
