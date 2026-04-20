package com.geosegbar.infra.map_kml.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MapKmlFolderUpdateDTO {

    @NotBlank(message = "Nome da pasta é obrigatório!")
    private String name;
}
