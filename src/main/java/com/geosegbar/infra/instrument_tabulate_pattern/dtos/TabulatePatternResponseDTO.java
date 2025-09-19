package com.geosegbar.infra.instrument_tabulate_pattern.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TabulatePatternResponseDTO {

    private Long id;
    private String name;
    private DamSummaryDTO dam;
    private FolderSummaryDTO folder;
    private List<InstrumentAssociationDTO> associations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamSummaryDTO {

        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderSummaryDTO {

        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstrumentAssociationDTO {

        private Long id;
        private Long instrumentId;
        private String instrumentName;
        private Boolean isDateEnable;
        private Integer dateIndex;
        private Boolean isHourEnable;
        private Integer hourIndex;
        private Boolean isUserEnable;
        private Integer userIndex;
        private Boolean isReadEnable;
        private List<OutputAssociationDTO> outputAssociations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputAssociationDTO {

        private Long id;
        private Long outputId;
        private String outputName;
        private String outputAcronym;
        private Integer outputIndex;
        private String customLabel;
        private Boolean isVisible;
        private MeasurementUnitDTO measurementUnit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeasurementUnitDTO {

        private Long id;
        private String name;
        private String acronym;
    }
}
