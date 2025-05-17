package com.geosegbar.infra.anomaly.dtos;

import java.util.List;

import com.geosegbar.common.enums.AnomalyOriginEnum;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDTO {

    @NotNull(message = "ID do usuário é obrigatório!")
    private Long userId;

    @NotNull(message = "ID da Barragem é obrigatório!")
    private Long damId;

    @NotNull(message = "Latitude é obrigatória!")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória!")
    private Double longitude;

    private Long questionnaireId;

    private Long questionId;

    @NotNull(message = "Origem da anomalia é obrigatório!")
    private AnomalyOriginEnum origin;

    private String observation;

    private String recommendation;

    private List<PhotoSubmissionDTO> photos;

    @NotNull(message = "Nível de Perigo é obrigatório!")
    private Long dangerLevelId;

    @NotNull(message = "ID do Status é obrigatório!")
    private Long statusId;
}
