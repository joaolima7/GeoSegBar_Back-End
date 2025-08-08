package com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTabulateFolderRequestDTO {

    @NotBlank(message = "Nome da pasta é obrigatório!")
    private String name;

    @NotNull(message = "ID da barragem é obrigatório!")
    private Long damId;
}
