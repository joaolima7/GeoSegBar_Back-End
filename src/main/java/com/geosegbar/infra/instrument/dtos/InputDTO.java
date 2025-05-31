package com.geosegbar.infra.instrument.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputDTO {

    @NotBlank(message = "Sigla do Input é obrigatória")
    private String acronym;

    @NotBlank(message = "Nome do Input é obrigatório")
    private String name;

    @NotNull(message = "Precisão do Input é obrigatória")
    private Integer precision;

    @NotNull(message = "ID da unidade de medida é obrigatório")
    private Long measurementUnitId;
}
