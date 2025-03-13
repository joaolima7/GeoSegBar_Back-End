package com.geosegbar.infra.checklist_submission.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class QuestionnaireResponseSubmissionDTO {
    @NotNull(message = "ID do modelo de questionário é obrigatório!")
    @Positive(message = "ID do modelo de questionário deve ser um número positivo!")
    private Long templateQuestionnaireId;
    
    @NotNull(message = "ID do usuário é obrigatório!")
    @Positive(message = "ID do usuário deve ser um número positivo!")
    private Long userId;
    
    @NotEmpty(message = "É necessário incluir pelo menos uma resposta!")
    @Valid
    private List<AnswerSubmissionDTO> answers;
}