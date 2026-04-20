package com.geosegbar.infra.map_kml.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapKmlFolderResponseDTO {

    private Long id;
    private Long damId;
    private String name;
    private LocalDateTime createdAt;
    private List<MapKmlFileResponseDTO> files;
}
