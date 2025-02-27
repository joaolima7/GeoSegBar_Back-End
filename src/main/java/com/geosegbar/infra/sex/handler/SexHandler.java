package com.geosegbar.infra.sex.handler;

import org.springframework.stereotype.Component;

import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.infra.sex.persistence.jpa.SexModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class SexHandler {
    private Long id;
    private String name;

    public SexHandler fromEntity(SexEntity entity) {
        return new SexHandler(
            entity.getId(),
            entity.getName()
        );
    }

    public SexHandler fromModel(SexModel model) {
        return new SexHandler(
            model.getId(),
            model.getName()
        );
    }

    public SexModel toModel() {
        return new SexModel(
            getId(),
            getName()
        );
    }

    public SexEntity toEntity() {
        return new SexEntity(
            getId(),
            getName()
        );
    }
}
