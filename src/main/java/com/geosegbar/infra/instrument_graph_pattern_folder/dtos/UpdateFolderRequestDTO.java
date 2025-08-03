package com.geosegbar.infra.instrument_graph_pattern_folder.dtos;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFolderRequestDTO {

    @NotBlank(message = "Nome da pasta é obrigatório!")
    private String name;

    private List<Long> patternIds;
}
