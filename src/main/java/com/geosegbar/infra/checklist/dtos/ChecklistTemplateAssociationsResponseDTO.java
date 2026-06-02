package com.geosegbar.infra.checklist.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateAssociationsResponseDTO {

    private Long checklistId;
    private List<Long> associatedTemplateIds;
    private List<Long> disassociatedTemplateIds;
    private Integer templateCount;
    private List<ChecklistTemplateAssociationItemDTO> items;
}
