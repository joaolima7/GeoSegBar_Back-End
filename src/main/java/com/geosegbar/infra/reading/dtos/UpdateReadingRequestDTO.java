package com.geosegbar.infra.reading.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReadingRequestDTO {

    private LocalDate date;
    private LocalTime hour;
    private Long userId;
    private Map<String, Double> inputValues;
    private String comment;
}
