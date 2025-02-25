package com.geosegbar.infra.address.dto;

import org.springframework.stereotype.Component;

import com.geosegbar.core.address.entities.AddressEntity;
import com.geosegbar.infra.address.persistence.jpa.AddressModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class AddressHandler {
    private Long id;
    private String street;
    private String neighborhood;
    private String number;
    private String city;
    private String state;
    private String zipCode;

    public AddressHandler fromEntity(AddressEntity entity) {
        return new AddressHandler(
            entity.getId(),
            entity.getStreet(),
            entity.getNeighborhood(),
            entity.getNumber(),
            entity.getCity(),
            entity.getState(),
            entity.getZipCode()
        );
    }

    public AddressHandler fromModel(AddressModel model) {
        return new AddressHandler(
            model.getId(),
            model.getStreet(),
            model.getNeighborhood(),
            model.getNumber(),
            model.getCity(),
            model.getState(),
            model.getZipCode()
        );
    }

    public  AddressModel toModel() {
        return new AddressModel(
            getId(),
            getStreet(),
            getNeighborhood(),
            getNumber(),
            getCity(),
            getState(),
            getZipCode()
        );
    }

    public AddressEntity toEntity() {
        return new AddressEntity(
            getId(),
            getStreet(),
            getNeighborhood(),
            getNumber(),
            getCity(),
            getState(),
            getZipCode()
        );
    }
}
