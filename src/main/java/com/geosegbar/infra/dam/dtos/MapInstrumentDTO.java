package com.geosegbar.infra.dam.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapInstrumentDTO {

    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private Long instrumentTypeId;
    private String instrumentTypeName;
}
