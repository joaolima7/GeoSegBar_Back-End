package com.geosegbar.infra.dam.handler;

import org.springframework.stereotype.Component;

import com.geosegbar.core.dam.entities.DamEntity;
import com.geosegbar.infra.dam.persistence.jpa.DamModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class DamHandler {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String acronym;

    public DamHandler fromEntity(DamEntity entity) {
        return new DamHandler(
            entity.getId(),
            entity.getName(),
            entity.getLatitude(),
            entity.getLongitude(),
            entity.getAcronym()
        );
    }

    public DamHandler fromModel(DamModel model) {
        return new DamHandler(
            model.getId(),
            model.getName(),
            model.getLatitude(),
            model.getLongitude(),
            model.getAcronym()
        );
    }

    public DamModel toModel() {
        return new DamModel(
            getId(),
            getName(),
            getLatitude(),
            getLongitude(),
            getAcronym()
        );
    }

    public DamEntity toEntity() {
        return new DamEntity(
            getId(),
            getName(),
            getLatitude(),
            getLongitude(),
            getAcronym()
        );
    }
}
