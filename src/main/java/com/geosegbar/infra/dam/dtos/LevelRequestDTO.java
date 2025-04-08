package com.geosegbar.infra.dam.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LevelRequestDTO {
    private Long id; // Se o level já existir
    
    @NotBlank(message = "O nome do nível é obrigatório!")
    private String name; // Se for criar um novo level
    
    @NotNull(message = "O valor do nível é obrigatório!")
    private Double value;
    
    @NotBlank(message = "A unidade do nível é obrigatória!")
    private String unitLevel;
}
