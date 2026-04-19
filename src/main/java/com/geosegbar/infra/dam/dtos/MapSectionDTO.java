package com.geosegbar.infra.dam.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapSectionDTO {

    private Long id;
    private String name;
    private Double firstVertexLatitude;
    private Double firstVertexLongitude;
    private Double secondVertexLatitude;
    private Double secondVertexLongitude;
}
