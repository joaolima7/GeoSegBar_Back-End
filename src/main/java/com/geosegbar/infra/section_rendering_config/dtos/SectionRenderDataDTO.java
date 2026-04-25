package com.geosegbar.infra.section_rendering_config.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SectionRenderDataDTO {

    private Long sectionId;
    private Long configId;

    private String soilLabel;
    private String soilColor;
    private String filterLabel;
    private String filterColor;
    private String rockLabel;
    private String rockColor;

    private String topElevationColor;
    private String bottomElevationColor;
    private String piezometricElevationColor;

    private Double axisXMin;
    private Double axisXMax;
    private Double axisYMin;
    private Double axisYMax;

    private Boolean showDamAxis = false;
    private Boolean showLastUpstreamReading = false;
    private Boolean showLastDownstreamReading = false;

    private List<SectionCustomLevelDTO> customLevels = new ArrayList<>();

    // Piezometers (Piezômetro / Indicador de Nível d'Água) of this section
    private List<SectionRenderInstrumentDTO> piezometers = new ArrayList<>();

    // Reservoirs of the dam (each with isSelected flag)
    private List<SectionRenderReservoirDTO> reservoirs = new ArrayList<>();

    // Upstream and downstream telemetric instruments of the dam
    private SectionRenderTelemetricInstrumentDTO upstreamInstrument;
    private SectionRenderTelemetricInstrumentDTO downstreamInstrument;
}
