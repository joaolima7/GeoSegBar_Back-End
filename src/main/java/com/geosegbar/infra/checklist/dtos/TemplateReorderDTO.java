package com.geosegbar.infra.checklist.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateReorderDTO {

    @NotEmpty(message = "Lista de templates é obrigatória!")
    @Valid
    private List<TemplateOrderDTO> templates;
}
