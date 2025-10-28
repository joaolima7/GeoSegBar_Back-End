package com.geosegbar.infra.reading.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.geosegbar.entities.ReadingEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupedReadingData {

    private LocalDate date;
    private LocalTime hour;
    private Map<String, Double> inputValues = new HashMap<>();
    private List<ReadingEntity> readings;
    private String comment;
    private String createdBy;
}
