package com.geosegbar.infra.map_kml.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameKmlFileRequest {

    @NotBlank(message = "O nome do arquivo não pode ser vazio.")
    private String filename;
}
