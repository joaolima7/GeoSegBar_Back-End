package com.geosegbar.infra.template_questionnaire_question.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionReorderDTO {
    
    @NotNull(message = "ID do template questionário é obrigatório")
    private Long templateQuestionnaireId;
    
    @NotEmpty(message = "É necessário informar as questões a serem reordenadas")
    @Valid
    private List<QuestionOrderDTO> questions;
}
