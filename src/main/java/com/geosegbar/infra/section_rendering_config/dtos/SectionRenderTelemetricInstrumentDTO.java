package com.geosegbar.infra.section_rendering_config.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionRenderTelemetricInstrumentDTO {
    private Long id;
    private String name;
    private Boolean isLinimetricRuler;
    private Boolean isDownstream;
    private LastReadingDTO lastReading;
}
