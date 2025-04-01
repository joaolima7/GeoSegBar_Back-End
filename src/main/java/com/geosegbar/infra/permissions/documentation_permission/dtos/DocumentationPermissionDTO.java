package com.geosegbar.infra.permissions.documentation_permission.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationPermissionDTO {
    
    private Long id;
    
    @NotNull(message = "ID do usuário é obrigatório")
    private Long userId;
    
    @NotNull(message = "Permissão de visualização de PSB é obrigatória")
    private Boolean viewPSB;
    
    @NotNull(message = "Permissão de edição de PSB é obrigatória")
    private Boolean editPSB;
    
    @NotNull(message = "Permissão de compartilhamento de PSB é obrigatória")
    private Boolean sharePSB;
}