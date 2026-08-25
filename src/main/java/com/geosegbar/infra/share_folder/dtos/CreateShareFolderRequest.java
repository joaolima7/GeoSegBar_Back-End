package com.geosegbar.infra.share_folder.dtos;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.geosegbar.configs.web_config.EndOfDayLocalDateTimeDeserializer;
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

    /**
     * Quando o link deixa de valer. Nulo = nunca expira.
     *
     * Data pura ("2026-08-26") é interpretada como o FIM daquele dia. O usuário
     * que escolhe 26/08 espera que o link funcione durante o dia 26; tomar como
     * 00:00 faria o link nascer vencido.
     */
    @JsonDeserialize(using = EndOfDayLocalDateTimeDeserializer.class)
    private LocalDateTime expiresAt;

    private String customMessage;
}
