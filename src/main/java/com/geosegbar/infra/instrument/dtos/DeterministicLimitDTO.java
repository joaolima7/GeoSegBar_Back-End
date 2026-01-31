package com.geosegbar.infra.instrument.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeterministicLimitDTO {

    private Long id;
    private BigDecimal attentionValue;
    private BigDecimal alertValue;
    private BigDecimal emergencyValue;
}
