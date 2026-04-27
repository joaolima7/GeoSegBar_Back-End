package com.geosegbar.infra.map_kml.dtos;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class KmlFeatureDTO {
    private Integer featureIndex;

    private String originalName;
    private String customName;
    private String customIconClass;
    private String customColor;

    private String type;
    private List<String> folderPath;

    private String styleColor;
    private String strokeColor;
    private String fillColor;
    private String iconHref;

    private JsonNode geometry;

    private Double minLat;
    private Double minLng;
    private Double maxLat;
    private Double maxLng;
}
