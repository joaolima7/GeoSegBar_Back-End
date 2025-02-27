package com.geosegbar.core.dam.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamEntity {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String acronym;
}
