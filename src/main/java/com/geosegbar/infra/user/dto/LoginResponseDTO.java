package com.geosegbar.infra.user.dto;

import com.geosegbar.entities.SexEntity;

public record LoginResponseDTO(Long id, String name, String email, String phone, SexEntity sex, String token) {
    
}
