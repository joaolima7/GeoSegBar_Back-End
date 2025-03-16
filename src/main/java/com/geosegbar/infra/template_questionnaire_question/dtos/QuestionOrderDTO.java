package com.geosegbar.infra.template_questionnaire_question.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOrderDTO {
    
    @NotNull(message = "ID da associação template-questão é obrigatório")
    private Long templateQuestionId;
    
    @NotNull(message = "Índice de ordem é obrigatório")
    @Min(value = 1, message = "A ordenação deve começar em 1")
    private Integer orderIndex;
}
