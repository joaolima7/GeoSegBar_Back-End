package com.geosegbar.infra.pae.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PAEProtectionElementDTO {

    private Long id;

    @NotBlank(message = "Nome do elemento de autoproteção é obrigatório!")
    private String name;

    @NotBlank(message = "Valor do elemento de autoproteção é obrigatório!")
    private String value;
}
