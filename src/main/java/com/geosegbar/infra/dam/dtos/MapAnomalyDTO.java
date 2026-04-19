package com.geosegbar.infra.dam.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapAnomalyDTO {

    private Long id;
    private Double latitude;
    private Double longitude;
    private Long questionId;
    private String questionText;
    private Long questionnaireId;
    private String questionnaireName;
    private String dangerLevelName;
    private String statusName;
}
