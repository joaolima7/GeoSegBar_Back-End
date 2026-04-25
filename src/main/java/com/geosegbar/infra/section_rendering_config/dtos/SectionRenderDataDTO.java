package com.geosegbar.infra.section_rendering_config.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SectionRenderDataDTO {

    private Long sectionId;
    private Long configId;

    // Legends
    private String soilLabel;
    private String soilColor;
    private String filterLabel;
    private String filterColor;
    private String rockLabel;
    private String rockColor;

    // Elevation colors
    private String topElevationColor;
    private String bottomElevationColor;
    private String piezometricElevationColor;

    // Axis limits
    private Double axisXMin;
    private Double axisXMax;
    private Double axisYMin;
    private Double axisYMax;

    // Display toggles
    private Boolean showDamAxis = false;
    private Boolean showLastUpstreamReading = false;
    private Boolean showLastDownstreamReading = false;
    private Boolean showMinNormalLevel = false;
    private Boolean showMaxNormalLevel = false;
    private Boolean showMaxMaximorumLevel = false;

    // Custom levels
    private List<SectionCustomLevelDTO> customLevels = new ArrayList<>();

    // Piezometers (Piezômetro / Indicador de Nível d'Água) of this section
    private List<SectionRenderInstrumentDTO> piezometers = new ArrayList<>();

    // Upstream and downstream telemetric instruments of the dam
    private SectionRenderTelemetricInstrumentDTO upstreamInstrument;
    private SectionRenderTelemetricInstrumentDTO downstreamInstrument;
}
