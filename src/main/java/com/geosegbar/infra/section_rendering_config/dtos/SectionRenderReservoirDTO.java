package com.geosegbar.infra.section_rendering_config.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionRenderReservoirDTO {
    private Long id;
    private Long levelId;
    private String levelName;
    private Double levelValue;
    private String levelUnit;
    private Boolean isSelected;
}
