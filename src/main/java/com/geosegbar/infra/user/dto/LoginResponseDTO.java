package com.geosegbar.infra.user.dto;

import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.entities.SexEntity;

public record LoginResponseDTO(Long id, String name, String email, String phone, SexEntity sex, RoleEnum role, String token) {
    
}
