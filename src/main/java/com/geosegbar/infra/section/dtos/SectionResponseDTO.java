package com.geosegbar.infra.section.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponseDTO {
    private Long id;
    private String name;
    private String filePath;
    private Double firstVertexLatitude;
    private Double secondVertexLatitude;
    private Double firstVertexLongitude;
    private Double secondVertexLongitude;
    private DamSummaryDTO dam;
}
