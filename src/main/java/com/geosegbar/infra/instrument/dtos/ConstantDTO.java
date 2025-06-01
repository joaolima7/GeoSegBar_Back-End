package com.geosegbar.infra.instrument.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConstantDTO {

    @NotBlank(message = "Sigla da Constante é obrigatória")
    private String acronym;

    @NotBlank(message = "Nome da Constante é obrigatório")
    private String name;

    @NotNull(message = "Precisão da Constante é obrigatória")
    private Integer precision;

    @NotNull(message = "Valor da Constante é obrigatório")
    private Double value;

    @NotNull(message = "ID da unidade de medida é obrigatório")
    private Long measurementUnitId;

    private String measurementUnitName;
    private String measurementUnitAcronym;
}
