package com.geosegbar.infra.template_questionnaire.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionAssociationItemDTO {

    private Long templateQuestionId;
    private Long questionId;
    private Integer orderIndex;
}
