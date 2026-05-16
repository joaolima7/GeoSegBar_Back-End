package com.geosegbar.infra.checklist.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateReorderDTO {

    @NotNull(message = "ID do checklist é obrigatório!")
    private Long checklistId;

    @NotEmpty(message = "Lista de templates é obrigatória!")
    @Valid
    private List<TemplateOrderDTO> templates;
}
