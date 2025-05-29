package com.geosegbar.infra.checklist_submission.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistResponseSubmissionDTO {

    @NotNull(message = "ID da barragem é obrigatório!")
    @Positive(message = "ID da barragem deve ser um número positivo!")
    private Long damId;

    @NotBlank(message = "Nome do checklist é obrigatório!")
    private String checklistName;

    @NotNull(message = "ID do checklist é obrigatório!")
    @Positive(message = "ID do checklist deve ser um número positivo!")
    private Long checklistId;

    @NotNull(message = "ID do usuário é obrigatório!")
    @Positive(message = "ID do usuário deve ser um número positivo!")
    private Long userId;

    @NotEmpty(message = "É necessário incluir pelo menos um questionário!")
    @Valid
    private List<QuestionnaireResponseSubmissionDTO> questionnaireResponses;

    private boolean isMobile;
}
