package com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFolderNameDTO {

    @NotBlank(message = "Nome da pasta é obrigatório!")
    private String name;
}
