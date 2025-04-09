package com.geosegbar.infra.psb.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadPSBFileRequest {
    @NotNull(message = "O arquivo é obrigatório")
    private MultipartFile file;
    
    @NotNull(message = "ID do usuário é obrigatório")
    private Long uploadedById;
}