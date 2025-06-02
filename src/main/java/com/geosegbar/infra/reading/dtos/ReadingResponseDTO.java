package com.geosegbar.infra.reading.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.infra.reading_input_value.dtos.ReadingInputValueDTO;

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
    private Double calculatedValue;
    private LimitStatusEnum limitStatus;
    private Long instrumentId;
    private String instrumentName;
    private Long outputId;
    private String outputName;
    private String outputAcronym;
    private List<ReadingInputValueDTO> inputValues;
}
