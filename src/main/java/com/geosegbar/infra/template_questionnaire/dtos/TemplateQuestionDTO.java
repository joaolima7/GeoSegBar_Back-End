package com.geosegbar.infra.template_questionnaire.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionDTO {
    
    @NotNull(message = "ID da questão é obrigatório!")
    private Long questionId;
    
    @NotNull(message = "Índice de ordem é obrigatório!")
    @PositiveOrZero(message = "Índice de ordem deve ser um número positivo ou zero!")
    private Integer orderIndex;
}