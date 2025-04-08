package com.geosegbar.infra.dam.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservoirRequestDTO {
    @NotNull(message = "As informações de nível são obrigatórias!")
    private LevelRequestDTO level;
}
