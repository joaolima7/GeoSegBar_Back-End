package com.geosegbar.infra.instrument_graph_pattern.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNameGraphPatternDTO {

    @NotBlank(message = "Nome do Padrão de Gráfico é obrigatório!")
    private String name;
}
