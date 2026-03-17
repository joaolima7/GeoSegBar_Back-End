package com.geosegbar.infra.dam.dtos;

import com.geosegbar.common.enums.StatusEnum;

public record DamQuickAccessDTO(
        Long damId,
        String damName,
        StatusEnum status,
        Long clientId,
        String clientName
        ) {

}
