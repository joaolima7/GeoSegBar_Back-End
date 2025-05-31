package com.geosegbar.infra.reading.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import com.geosegbar.common.enums.LimitStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingResponseDTO {

    private Long id;
    private LocalDate date;
    private LocalTime hour;
    private Double value;
    private LimitStatusEnum limitStatus;
    private Long instrumentId;
    private String instrumentName;
}
