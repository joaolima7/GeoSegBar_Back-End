package com.geosegbar.infra.instrument_graph_axes.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGraphAxesRequestDTO {

    @NotNull(message = "Tamanho da fonte da abcissa é obrigatório!")
    private Integer abscissaPx;

    @NotNull(message = "Linhas de grade da abcissa é obrigatório!")
    private Boolean abscissaGridLinesEnable;

    @NotNull(message = "Tamanho da fonte da ordenada primária é obrigatório!")
    private Integer primaryOrdinatePx;

    @NotNull(message = "Tamanho da fonte da ordenada secundária é obrigatório!")
    private Integer secondaryOrdinatePx;

    @NotNull(message = "Linhas de grade da ordenada primária é obrigatório!")
    private Boolean primaryOrdinateGridLinesEnable;

    private String primaryOrdinateTitle;
    private String secondaryOrdinateTitle;
    private Double primaryOrdinateSpacing;
    private Double secondaryOrdinateSpacing;
    private Double primaryOrdinateInitialValue;
    private Double secondaryOrdinateInitialValue;
    private Double primaryOrdinateMaximumValue;
    private Double secondaryOrdinateMaximumValue;
}
