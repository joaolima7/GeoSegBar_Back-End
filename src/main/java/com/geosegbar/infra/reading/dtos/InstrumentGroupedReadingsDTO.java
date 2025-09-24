package com.geosegbar.infra.reading.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentGroupedReadingsDTO {

    private Long instrumentId;
    private String instrumentName;
    private String instrumentType;
    private Long damId;
    private String damName;
    private List<GroupedDateHourReadingsDTO> groupedReadings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupedDateHourReadingsDTO {

        private String dateTime;
        private List<ReadingResponseDTO> readings;
    }
}
