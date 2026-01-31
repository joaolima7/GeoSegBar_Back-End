package com.geosegbar.infra.instrument.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticalLimitDTO {

    private Long id;
    private BigDecimal lowerValue;
    private BigDecimal upperValue;
}
