package com.geosegbar.infra.user.dto;

/**
 * Projeção mínima usada exclusivamente para montar o email de suporte. Busca
 * apenas os 4 campos necessários — sem carregar entidades completas.
 */
public interface UserSupportInfoProjection {

    String getName();

    String getEmail();

    String getPhone();

    String getClientName();
}
