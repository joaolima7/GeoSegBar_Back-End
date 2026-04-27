package com.geosegbar.infra.map_kml.processing;

import java.util.List;

import lombok.Data;

@Data
public class KmlRawFeature {
    private int featureIndex;
    private String externalId;
    private String name;
    private String type; // POINT, LINE, POLYGON
    private List<String> folderPath;
    private String styleColor;
    private String strokeColor;
    private String fillColor;
    private String iconHref;
    private String geometryJson;
    private double minLat;
    private double minLng;
    private double maxLat;
    private double maxLng;
}
