package com.geosegbar.infra.checklist.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateAssociationsRequestDTO {

    private List<Long> associateTemplateIds;

    private List<Long> disassociateTemplateIds;

    @NotNull(message = "Lista de ordenação final é obrigatória!")
    @Valid
    private List<TemplateOrderDTO> order;
}
