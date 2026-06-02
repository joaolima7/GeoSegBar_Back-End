package com.geosegbar.infra.template_questionnaire.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionAssociationsResponseDTO {

    private Long templateId;
    private List<Long> associatedQuestionIds;
    private List<Long> disassociatedQuestionIds;
    private Integer questionCount;
    private List<TemplateQuestionAssociationItemDTO> items;
}
