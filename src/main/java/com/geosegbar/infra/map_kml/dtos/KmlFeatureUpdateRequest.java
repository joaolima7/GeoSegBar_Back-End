package com.geosegbar.infra.map_kml.dtos;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class KmlFeatureUpdateRequest {
    private String customName;
    private String customIconClass;
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Cor deve estar no formato hexadecimal válido!")
    private String customColor;
}
