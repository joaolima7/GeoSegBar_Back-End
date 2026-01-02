package com.geosegbar.infra.template_questionnaire.dtos;

import java.util.Set;

import com.geosegbar.common.enums.TypeQuestionEnum;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionDTO {

    private Long questionId;

    private String questionText;
    private TypeQuestionEnum type;
    private Long clientId;
    private Set<Long> optionIds;

    @NotNull(message = "Índice de ordem é obrigatório!")
    @PositiveOrZero(message = "Índice de ordem deve ser um número positivo ou zero!")
    private Integer orderIndex;

    /**
     * Verifica se é para criar uma nova questão
     */
    public boolean isNewQuestion() {
        return questionId == null && questionText != null;
    }

    public boolean isExistingQuestion() {
        return questionId != null;
    }
}
