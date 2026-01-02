package com.geosegbar.infra.checklist.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistCompleteCreationDTO {

    @NotBlank(message = "Nome do checklist é obrigatório!")
    private String name;

    @NotNull(message = "ID da barragem é obrigatório!")
    private Long damId;

    @NotEmpty(message = "Checklist deve ter pelo menos um template!")
    @Valid
    private List<TemplateInChecklistDTO> templates;
}
