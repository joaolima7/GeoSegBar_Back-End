package com.geosegbar.infra.pae.dtos;

import com.geosegbar.common.enums.PAEZoneTypeEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PAEContactDTO {

    private Long id;

    @NotNull(message = "Zona do contato é obrigatória!")
    private PAEZoneTypeEnum zone;

    private String name;

    private String role;

    private String city;

    private String state;

    private String phone;

    @Email(message = "Email do contato inválido!")
    private String email;
}
