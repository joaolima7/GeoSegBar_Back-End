package com.geosegbar.infra.template_questionnaire.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionnaireCreationDTO {

    @NotBlank(message = "Nome do Modelo de Questionário é obrigatório!")
    private String name;

    @NotNull(message = "ID da barragem é obrigatório!")
    private Long damId;

    @NotEmpty(message = "É necessário incluir pelo menos uma questão!")
    @Valid
    private List<TemplateQuestionDTO> questions;
}
