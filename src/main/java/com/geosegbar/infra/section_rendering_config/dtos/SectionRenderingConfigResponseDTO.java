package com.geosegbar.infra.section_rendering_config.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SectionRenderingConfigResponseDTO {

    private Long id;
    private Long sectionId;

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
    private List<Long> selectedInstrumentIds = new ArrayList<>();
    private List<Long> selectedReservoirIds = new ArrayList<>();
}
