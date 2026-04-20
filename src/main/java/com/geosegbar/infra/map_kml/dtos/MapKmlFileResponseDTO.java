package com.geosegbar.infra.map_kml.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapKmlFileResponseDTO {

    private Long id;
    private String filename;
    private String downloadUrl;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
}
