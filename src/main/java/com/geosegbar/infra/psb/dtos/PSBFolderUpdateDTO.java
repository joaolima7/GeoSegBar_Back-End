package com.geosegbar.infra.psb.dtos;

import com.geosegbar.common.enums.FolderColorEnum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PSBFolderUpdateDTO {

    private Long id;

    @NotBlank(message = "O nome da pasta é obrigatório")
    private String name;

    @NotNull(message = "O índice da pasta é obrigatório")
    @Min(value = 1, message = "O índice deve ser um número positivo")
    private Integer folderIndex;

    private String description;

    private FolderColorEnum color = FolderColorEnum.BLUE;
}
