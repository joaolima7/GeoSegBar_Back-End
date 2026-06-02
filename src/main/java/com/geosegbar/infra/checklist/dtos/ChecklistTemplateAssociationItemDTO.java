package com.geosegbar.infra.checklist.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateAssociationItemDTO {

    private Long checklistTemplateId;
    private Long templateId;
    private Integer orderIndex;
}
