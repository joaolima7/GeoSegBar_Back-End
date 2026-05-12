package com.geosegbar.infra.checklist.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChecklistNameUpdateDTO {

    @NotBlank(message = "Nome do checklist é obrigatório!")
    private String name;
}
