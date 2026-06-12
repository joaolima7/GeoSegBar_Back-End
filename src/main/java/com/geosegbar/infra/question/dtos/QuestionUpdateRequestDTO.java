package com.geosegbar.infra.question.dtos;

import com.geosegbar.entities.QuestionEntity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para atualização de uma questão com controle de propagação.
 *
 * <p>
 * Quando {@code applyToAll} é {@code true}, a edição é aplicada na própria
 * questão e reflete em todos os questionários (templates) que a utilizam.
 *
 * <p>
 * Quando {@code applyToAll} é {@code false} e a questão está em outros
 * questionários do mesmo cliente além do {@code templateId} informado, uma nova
 * questão é criada com as alterações e substitui a antiga apenas no
 * {@code templateId} (na mesma posição). As demais associações mantêm a questão
 * original. Se a questão não estiver em nenhum outro questionário, a edição é
 * aplicada in-place mesmo com {@code applyToAll=false} (não há motivo para
 * duplicar).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionUpdateRequestDTO {

    @NotNull(message = "Dados da questão são obrigatórios!")
    @Valid
    private QuestionEntity question;

    /**
     * Indica se a edição deve refletir em todos os questionários que usam a
     * questão. Default {@code true} (comportamento retrocompatível).
     */
    private Boolean applyToAll = true;

    /**
     * Template de origem da edição. Obrigatório apenas quando
     * {@code applyToAll=false} e a questão for compartilhada — é nele que a
     * cópia substitui a questão original.
     */
    private Long templateId;
}
