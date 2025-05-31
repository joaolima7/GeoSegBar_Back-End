package com.geosegbar.infra.instrument.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticalLimitDTO {

    private Double lowerValue;
    private Double upperValue;
}
