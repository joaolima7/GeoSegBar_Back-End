package com.geosegbar.infra.template_questionnaire.dtos;

import com.geosegbar.common.enums.AssociationAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionAssociationResponseDTO {

    private Long templateId;
    private Long questionId;
    private Long templateQuestionId;
    private Integer orderIndex;
    private AssociationAction action;
}
