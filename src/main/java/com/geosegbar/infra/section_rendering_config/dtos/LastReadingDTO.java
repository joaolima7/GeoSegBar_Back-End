package com.geosegbar.infra.section_rendering_config.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.geosegbar.common.enums.LimitStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LastReadingDTO {
    private LocalDate date;
    private LocalTime hour;
    private BigDecimal calculatedValue;
    private LimitStatusEnum limitStatus;
}
