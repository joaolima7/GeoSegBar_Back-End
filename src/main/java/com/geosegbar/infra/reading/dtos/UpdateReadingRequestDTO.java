package com.geosegbar.infra.reading.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

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
}
