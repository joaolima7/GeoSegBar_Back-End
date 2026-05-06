package com.geosegbar.infra.section_rendering_config.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SectionRenderInstrumentDTO {
    private Long id;
    private String name;
    private Double distanceOffset;
    private Boolean isSelected;

    // Cota de topo / cota de boca  (prioridade: constante > output)
    private ElevationVariableDTO topElevation;

    // Cota de fundo / cota de instalação  (prioridade: constante > output)
    private ElevationVariableDTO bottomElevation;

    // Cota piezométrica  (prioridade: output > constante)
    private ElevationVariableDTO piezometricElevation;
}
