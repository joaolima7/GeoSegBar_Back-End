package com.geosegbar.infra.instrument.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeterministicLimitDTO {

    private Double attentionValue;
    private Double alertValue;
    private Double emergencyValue;
}
