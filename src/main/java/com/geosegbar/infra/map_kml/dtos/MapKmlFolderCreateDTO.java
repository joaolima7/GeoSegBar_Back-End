package com.geosegbar.infra.map_kml.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MapKmlFolderCreateDTO {

    @NotNull(message = "O ID da barragem é obrigatório!")
    private Long damId;

    @NotBlank(message = "Nome da pasta é obrigatório!")
    private String name;
}
