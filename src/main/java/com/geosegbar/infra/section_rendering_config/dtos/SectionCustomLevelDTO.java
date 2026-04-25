package com.geosegbar.infra.section_rendering_config.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionCustomLevelDTO {

    private Long id;

    @NotBlank(message = "Nome do nível personalizado é obrigatório!")
    private String name;

    @NotNull(message = "Valor do nível personalizado é obrigatório!")
    private BigDecimal value;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Cor deve estar no formato hexadecimal válido!")
    private String color;

    private Boolean enabled = true;
}
