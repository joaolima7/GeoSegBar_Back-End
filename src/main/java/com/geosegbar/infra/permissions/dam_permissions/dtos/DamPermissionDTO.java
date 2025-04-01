package com.geosegbar.infra.permissions.dam_permissions.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamPermissionDTO {
    
    @NotNull(message = "ID do usuário é obrigatório!")
    private Long userId;
    
    @NotNull(message = "ID da barragem é obrigatório!")
    private Long damId;
    
    @NotNull(message = "ID do cliente é obrigatório!")
    private Long clientId;
    
    @NotNull(message = "Status de acesso é obrigatório!")
    private Boolean hasAccess;
}
