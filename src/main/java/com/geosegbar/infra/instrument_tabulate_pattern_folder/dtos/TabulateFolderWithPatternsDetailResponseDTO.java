package com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos;

import java.util.List;

import com.geosegbar.infra.instrument_tabulate_pattern.dtos.TabulatePatternResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TabulateFolderWithPatternsDetailResponseDTO {

    private Long id;
    private String name;
    private DamDetailDTO dam;
    private List<TabulatePatternResponseDTO> patterns;

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
