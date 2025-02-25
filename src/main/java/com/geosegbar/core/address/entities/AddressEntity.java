package com.geosegbar.core.address.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressEntity {
    private Long id;
    private String street;
    private String neighborhood;
    private String number;
    private String city;
    private String state;
    private String zipCode;
}

