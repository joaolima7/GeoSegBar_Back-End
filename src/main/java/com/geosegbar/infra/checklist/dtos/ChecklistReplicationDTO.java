package com.geosegbar.infra.checklist.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistReplicationDTO {

    @NotNull(message = "ID do checklist de origem é obrigatório!")
    @Positive(message = "ID do checklist de origem deve ser um número positivo!")
    private Long sourceChecklistId;

    @NotNull(message = "ID da barragem de destino é obrigatório!")
    @Positive(message = "ID da barragem de destino deve ser um número positivo!")
    private Long targetDamId;
}
