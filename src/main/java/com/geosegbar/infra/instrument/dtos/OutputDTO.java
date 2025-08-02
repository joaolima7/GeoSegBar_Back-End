package com.geosegbar.infra.instrument.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutputDTO {

    private Long id;

    @NotBlank(message = "Sigla do Output é obrigatória")
    private String acronym;

    @NotBlank(message = "Nome do Output é obrigatório")
    private String name;

    @NotBlank(message = "Equação do Output é obrigatória")
    private String equation;

    @NotNull(message = "Precisão do Output é obrigatória")
    private Integer precision;

    @NotNull(message = "ID da unidade de medida é obrigatório")
    private Long measurementUnitId;

    private StatisticalLimitDTO statisticalLimit;
    private DeterministicLimitDTO deterministicLimit;

    private String measurementUnitName;
    private String measurementUnitAcronym;
}
