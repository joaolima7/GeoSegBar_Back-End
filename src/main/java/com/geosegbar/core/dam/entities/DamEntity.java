package com.geosegbar.core.dam.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DamEntity {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String acronym;
}
