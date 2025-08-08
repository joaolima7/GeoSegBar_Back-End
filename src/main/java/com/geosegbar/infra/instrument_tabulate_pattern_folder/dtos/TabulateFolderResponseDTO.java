package com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TabulateFolderResponseDTO {

    private Long id;
    private String name;
    private DamSummaryDTO dam;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamSummaryDTO {

        private Long id;
        private String name;
    }
}
