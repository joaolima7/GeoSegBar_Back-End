package com.geosegbar.infra.instrument_graph_pattern_folder.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderDetailResponseDTO {

    private Long id;
    private String name;
    private DamDetailDTO dam;
    private List<PatternSummaryDTO> patterns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamDetailDTO {

        private Long id;
        private String name;
        private String city;
        private String state;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatternSummaryDTO {

        private Long id;
        private String name;
        private InstrumentSummaryDTO instrument;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstrumentSummaryDTO {

        private Long id;
        private String name;
        private String location;
    }
}
