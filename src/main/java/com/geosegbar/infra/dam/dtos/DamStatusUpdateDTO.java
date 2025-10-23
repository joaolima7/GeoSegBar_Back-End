package com.geosegbar.infra.dam.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamStatusUpdateDTO {

    @NotNull(message = "ID do status é obrigatório")
    private Long statusId;
}
