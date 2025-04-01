package com.geosegbar.infra.permissions.instrumentation_permission.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstrumentationPermissionDTO {
    
    private Long id;
    
    @NotNull(message = "O ID do usuário é obrigatório")
    private Long userId;
    
    private Boolean viewGraphs = false;
    
    private Boolean editGraphsLocal = false;
    
    private Boolean editGraphsDefault = false;
    
    private Boolean viewRead = false;
    
    private Boolean editRead = false;
    
    private Boolean viewSections = false;
    
    private Boolean editSections = false;
}
