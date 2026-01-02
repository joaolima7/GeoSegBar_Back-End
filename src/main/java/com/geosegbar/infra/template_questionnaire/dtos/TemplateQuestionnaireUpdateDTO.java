package com.geosegbar.infra.template_questionnaire.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionnaireUpdateDTO {

    @NotBlank(message = "Nome do Modelo de Questionário é obrigatório!")
    private String name;

    @NotEmpty(message = "É necessário incluir pelo menos uma questão!")
    @Valid
    private List<TemplateQuestionDTO> questions;
}
