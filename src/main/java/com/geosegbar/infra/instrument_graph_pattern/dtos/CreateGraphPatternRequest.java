package com.geosegbar.infra.instrument_graph_pattern.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGraphPatternRequest {

    @NotNull(message = "ID do instrumento é obrigatório!")
    private Long instrumentId;

    @NotBlank(message = "Nome do Padrão de Gráfico é obrigatório!")
    private String name;

    private Long folderId;
}
