package com.geosegbar.infra.psb.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para iniciar upload via URL pré-assinada. Laravel envia os metadados
 * do arquivo; Spring retorna URL(s) para upload direto ao S3.
 */
@Data
public class PresignedUploadInitRequest {

    @NotBlank(message = "O nome do arquivo é obrigatório")
    private String filename;

    @NotNull(message = "O tamanho do arquivo é obrigatório")
    @Min(value = 1, message = "O tamanho deve ser maior que zero")
    private Long fileSize;

    @NotBlank(message = "O content type é obrigatório")
    private String contentType;

    @NotNull(message = "O ID do usuário é obrigatório")
    private Long uploadedById;
}
