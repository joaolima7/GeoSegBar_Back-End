package com.geosegbar.infra.anomaly.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAnomalyRequestDTO {

    private String observation;
    private String recommendation;
    private Long dangerLevelId;
    private Long statusId;
}
