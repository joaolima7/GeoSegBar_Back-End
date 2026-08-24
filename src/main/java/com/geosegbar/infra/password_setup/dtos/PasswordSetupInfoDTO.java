package com.geosegbar.infra.password_setup.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dados mínimos devolvidos ao validar o link de definição de senha — apenas o
 * suficiente para a tela cumprimentar o usuário e mostrar em qual conta a senha
 * será definida. Nenhuma credencial trafega aqui.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordSetupInfoDTO {

    private String name;
    private String email;
}
