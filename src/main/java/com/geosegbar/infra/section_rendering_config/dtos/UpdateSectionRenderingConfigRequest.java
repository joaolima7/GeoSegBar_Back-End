package com.geosegbar.infra.section_rendering_config.dtos;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateSectionRenderingConfigRequest {

    private static final String HEX = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
    private static final String HEX_MSG = "Cor deve estar no formato hexadecimal válido!";

    private String soilLabel;
    @Pattern(regexp = HEX, message = HEX_MSG)
    private String soilColor;

    private String filterLabel;
    @Pattern(regexp = HEX, message = HEX_MSG)
    private String filterColor;

    private String rockLabel;
    @Pattern(regexp = HEX, message = HEX_MSG)
    private String rockColor;

    @Pattern(regexp = HEX, message = HEX_MSG)
    private String topElevationColor;
    @Pattern(regexp = HEX, message = HEX_MSG)
    private String bottomElevationColor;
    @Pattern(regexp = HEX, message = HEX_MSG)
    private String piezometricElevationColor;

    private Double axisXMin;
    private Double axisXMax;
    private Double axisYMin;
    private Double axisYMax;

    private Boolean showDamAxis = false;
    private Boolean showLastUpstreamReading = false;
    private Boolean showLastDownstreamReading = false;

    @Valid
    private List<SectionCustomLevelDTO> customLevels = new ArrayList<>();

    private List<Long> selectedInstrumentIds = new ArrayList<>();

    private List<Long> selectedReservoirIds = new ArrayList<>();
}
