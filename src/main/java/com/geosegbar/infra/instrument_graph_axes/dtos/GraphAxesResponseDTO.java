package com.geosegbar.infra.instrument_graph_axes.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphAxesResponseDTO {

    private Long id;
    private Long patternId;
    private Integer abscissaPx;
    private Boolean abscissaGridLinesEnable;
    private Integer primaryOrdinatePx;
    private Integer secondaryOrdinatePx;
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
