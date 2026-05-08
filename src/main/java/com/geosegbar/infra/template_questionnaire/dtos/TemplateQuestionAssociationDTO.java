package com.geosegbar.infra.template_questionnaire.dtos;

import com.geosegbar.common.enums.AssociationAction;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionAssociationDTO {

    @NotNull(message = "ID da questao e obrigatorio!")
    private Long questionId;

    private Integer orderIndex;

    @NotNull(message = "Acao e obrigatoria!")
    private AssociationAction action;
}
