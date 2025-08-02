package com.geosegbar.infra.instrument_graph_pattern.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphPatternResponseDTO {

    private Long id;
    private String name;
    private Long instrumentId;
}
