package com.geosegbar.infra.dam.dtos;

import com.geosegbar.common.enums.StatusEnum;

/**
 * Barragem que o usuário do token pode acessar.
 *
 * É o DamQuickAccessDTO mais city, state, latitude e longitude — os campos que
 * faltavam ao app e o obrigavam a baixar a lista completa por cliente, que não
 * filtra por permissão, só para depois descartar o que não podia ver. O dado
 * de barragem não permitida chegava a ficar gravado no aparelho.
 */
public record DamAccessibleDTO(
        Long damId,
        String damName,
        StatusEnum status,
        Long clientId,
        String clientName,
        String city,
        String state,
        Double latitude,
        Double longitude
        ) {

}
