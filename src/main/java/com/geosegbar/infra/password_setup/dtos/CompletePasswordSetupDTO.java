package com.geosegbar.infra.password_setup.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletePasswordSetupDTO {

    @NotBlank(message = "O token é obrigatório!")
    private String token;

    @NotBlank(message = "A nova senha não pode estar em branco!")
    @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres!")
    private String newPassword;
}
