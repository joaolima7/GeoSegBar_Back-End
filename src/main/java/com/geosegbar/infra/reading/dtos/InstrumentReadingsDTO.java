package com.geosegbar.infra.reading.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentReadingsDTO {

    private Long instrumentId;
    private String instrumentName;
    private String instrumentType;
    private List<ReadingResponseDTO> readings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultiInstrumentReadingsResponseDTO {

        private List<InstrumentReadingsDTO> instrumentsReadings;
        private int pageSize;
        private long totalInstruments;
    }
}
