package com.geosegbar.infra.instrument_graph_pattern_folder.dtos;

import java.util.List;

import com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternDetailResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderWithPatternsDetailResponseDTO {

    private Long id;
    private String name;
    private DamDetailDTO dam;
    private List<GraphPatternDetailResponseDTO> patterns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamDetailDTO {

        private Long id;
        private String name;
        private String city;
        private String state;
    }
}
