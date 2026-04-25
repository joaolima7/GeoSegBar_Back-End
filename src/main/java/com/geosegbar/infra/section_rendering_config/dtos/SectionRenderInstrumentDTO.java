package com.geosegbar.infra.section_rendering_config.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionRenderInstrumentDTO {
    private Long id;
    private String name;
    private Double distanceOffset;
    private Boolean isSelected;
    private LastReadingDTO lastReading;
}
