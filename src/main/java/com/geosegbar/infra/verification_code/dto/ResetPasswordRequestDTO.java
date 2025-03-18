package com.geosegbar.infra.verification_code.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequestDTO {
    @Email(message = "Email inválido!")
    @NotBlank(message = "Email é obrigatório!")
    private String email;
    
    @NotBlank(message = "Código de verificação é obrigatório!")
    private String code;
    
    @NotBlank(message = "Nova senha é obrigatória!")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres!")
    private String newPassword;
}