package com.geosegbar.infra.share_folder.dtos;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateShareFolderRequest {

    @NotNull(message = "ID da pasta PSB é obrigatório")
    private Long psbFolderId;

    @NotNull(message = "ID do usuário que compartilha é obrigatório")
    private Long sharedById;

    @NotBlank(message = "Email do destinatário é obrigatório")
    @Email(message = "Email inválido")
    private String sharedWithEmail;

    private LocalDateTime expiresAt;

    private String customMessage;
}
