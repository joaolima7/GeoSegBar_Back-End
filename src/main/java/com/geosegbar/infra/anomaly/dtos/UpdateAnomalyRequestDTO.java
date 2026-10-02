package com.geosegbar.infra.anomaly.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Campo nulo significa "não mexer" — é um PATCH. Para desvincular a anomalia de
 * uma pergunta ou de um questionário, enviar zero.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAnomalyRequestDTO {

    private String observation;
    private String recommendation;
    private Long dangerLevelId;
    private Long statusId;
    private Long questionId;
    private Long questionnaireId;
    private Double latitude;
    private Double longitude;
}
