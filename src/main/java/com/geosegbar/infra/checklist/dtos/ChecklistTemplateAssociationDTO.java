package com.geosegbar.infra.checklist.dtos;

import com.geosegbar.common.enums.AssociationAction;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateAssociationDTO {

    @NotNull(message = "ID do template e obrigatorio!")
    private Long templateId;

    @NotNull(message = "Acao e obrigatoria!")
    private AssociationAction action;
}
