package com.geosegbar.infra.reading.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingGroupDTO {
    private LocalDate date;
    private LocalTime hour;
    private List<ReadingResponseDTO> readings; 
}