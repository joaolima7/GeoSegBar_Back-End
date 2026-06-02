package com.geosegbar.infra.checklist.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateOrderDTO {

    @NotNull(message = "ID do template é obrigatório!")
    private Long templateId;

    @NotNull(message = "Índice de ordem é obrigatório!")
    @Min(value = 1, message = "Índice de ordem deve ser maior ou igual a 1!")
    private Integer orderIndex;
}
