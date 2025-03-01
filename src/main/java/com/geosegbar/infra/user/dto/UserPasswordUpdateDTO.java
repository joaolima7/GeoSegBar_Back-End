package com.geosegbar.infra.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPasswordUpdateDTO {
    @NotBlank(message = "A senha atual é obrigatória!")
    private String currentPassword;
    
    @NotBlank(message = "A nova senha não pode estar em branco!")
    @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres!")
    private String newPassword;
}
