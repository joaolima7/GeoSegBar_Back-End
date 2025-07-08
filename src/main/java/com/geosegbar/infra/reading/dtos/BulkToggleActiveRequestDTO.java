package com.geosegbar.infra.reading.dtos;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkToggleActiveRequestDTO {

    @NotNull(message = "O status de ativação é obrigatório")
    private Boolean active;

    @NotEmpty(message = "A lista de IDs não pode estar vazia")
    private List<Long> readingIds;
}
