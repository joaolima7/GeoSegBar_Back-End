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

    /**
     * Opcional apenas no fluxo de primeiro acesso, quando o próprio usuário troca
     * a senha temporária. Em qualquer outra troca continua sendo obrigatória — a
     * exigência é validada em UserService.updatePassword, não aqui, porque depende
     * do estado do usuário autenticado.
     */
    private String currentPassword;
    
    @NotBlank(message = "A nova senha não pode estar em branco!")
    @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres!")
    private String newPassword;
}
