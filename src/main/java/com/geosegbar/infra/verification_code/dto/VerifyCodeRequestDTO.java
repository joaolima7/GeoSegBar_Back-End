package com.geosegbar.infra.verification_code.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyCodeRequestDTO {
    @Email(message = "Email inválido!")
    @NotBlank(message = "Email é obrigatório!")
    private String email;
    
    @NotBlank(message = "Código de verificação é obrigatório!")
    private String code;
}
