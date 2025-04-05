package com.geosegbar.infra.user.dto;

import com.geosegbar.entities.RoleEntity;
import com.geosegbar.entities.SexEntity;
import com.geosegbar.entities.StatusEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {
    @NotBlank(message = "Nome é obrigatório!")
    private String name;

    @NotBlank(message = "Email é obrigatório!")
    @Email(message = "Email inválido!")
    private String email;

    @Pattern(regexp = "^(\\d{10,11})?$", message = "O telefone deve conter 10 ou 11 dígitos numéricos ou ser vazio!")
    private String phone;

    @NotNull(message = "O sexo deve ser informado!")
    private SexEntity sex;

    @NotNull(message = "O status deve ser informado!")
    private StatusEntity status;

    @NotNull(message = "A role deve ser informada!")
    private RoleEntity role;
}
