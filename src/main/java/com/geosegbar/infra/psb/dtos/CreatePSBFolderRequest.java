package com.geosegbar.infra.psb.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePSBFolderRequest {
    
    @NotBlank(message = "O nome da pasta é obrigatório")
    private String name;
    
    @NotNull(message = "O índice da pasta é obrigatório")
    @Min(value = 1, message = "O índice deve ser um número positivo")
    private Integer folderIndex;
    
    private String description;
    
    @NotNull(message = "ID da barragem é obrigatório")
    private Long damId;

    private Long createdById;
}