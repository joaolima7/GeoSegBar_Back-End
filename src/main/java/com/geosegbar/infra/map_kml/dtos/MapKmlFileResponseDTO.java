package com.geosegbar.infra.map_kml.dtos;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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
    private String processStatus;
    private Integer featureCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<KmlFeatureDTO> features;
}
