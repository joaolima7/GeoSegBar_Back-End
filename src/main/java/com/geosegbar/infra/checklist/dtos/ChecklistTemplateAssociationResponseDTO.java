package com.geosegbar.infra.checklist.dtos;

import com.geosegbar.common.enums.AssociationAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateAssociationResponseDTO {

    private Long checklistId;
    private Long templateId;
    private AssociationAction action;
    private Integer templateCount;
}
