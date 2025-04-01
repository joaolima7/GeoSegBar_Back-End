package com.geosegbar.infra.permissions.routine_inspection_permission.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoutineInspectionPermissionDTO {
    
    private Long id;
    
    @NotNull(message = "O ID do usuário é obrigatório")
    private Long userId;
    
    private Boolean isFillWeb = false;
    
    private Boolean isFillMobile = false;
}
