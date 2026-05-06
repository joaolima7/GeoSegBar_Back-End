package com.geosegbar.infra.section_rendering_config.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.geosegbar.common.enums.LimitStatusEnum;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ElevationVariableDTO {

    public enum VariableType { CONSTANT, OUTPUT }

    private Long id;
    private String name;
    private String acronym;
    private Integer precision;
    private InstrumentMeasurementUnitDTO measurementUnit;
    private VariableType type;

    // Preenchido apenas quando type = CONSTANT
    private Double constantValue;

    // Preenchido apenas quando type = OUTPUT (última leitura ativa)
    private LocalDate lastReadingDate;
    private LocalTime lastReadingHour;
    private BigDecimal lastReadingValue;
    private LimitStatusEnum lastReadingLimitStatus;
}
