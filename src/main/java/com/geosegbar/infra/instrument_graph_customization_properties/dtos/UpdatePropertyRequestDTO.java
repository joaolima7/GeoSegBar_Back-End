package com.geosegbar.infra.instrument_graph_customization_properties.dtos;

import com.geosegbar.common.enums.LineTypeEnum;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePropertyRequestDTO {

    private String name;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Cor deve estar no formato hexadecimal válido!")
    private String fillColor;

    private LineTypeEnum lineType;

    @NotNull(message = "Campo 'Exibir Label' é obrigatório!")
    private Boolean labelEnable;

    @NotNull(message = "Campo 'Ordenada Primária' é obrigatório!")
    private Boolean isPrimaryOrdinate;
}
