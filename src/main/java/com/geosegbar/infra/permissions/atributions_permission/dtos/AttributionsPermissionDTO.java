package com.geosegbar.infra.permissions.atributions_permission.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributionsPermissionDTO {
    
    private Long id;
    
    @NotNull(message = "ID do usuário é obrigatório")
    private Long userId;
    
    private Boolean editUser = false;
    
    private Boolean editDam = false;
    
    private Boolean editGeralData = false;
}
