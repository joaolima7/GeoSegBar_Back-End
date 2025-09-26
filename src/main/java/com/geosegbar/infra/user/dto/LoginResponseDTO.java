package com.geosegbar.infra.user.dto;

import java.util.List;

import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.SexEntity;

public record LoginResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        SexEntity sex,
        RoleEnum role,
        Boolean isFirstAccess,
        String token,
        List<ClientEntity> clients
        ) {

}
