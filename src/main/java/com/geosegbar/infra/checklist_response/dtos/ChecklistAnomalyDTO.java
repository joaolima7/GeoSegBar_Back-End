package com.geosegbar.infra.checklist_response.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistAnomalyDTO {

    private Long id;
    private Long questionId;
    private Long questionnaireId;
    private String observation;
    private String recommendation;
    private Double latitude;
    private Double longitude;
    private Long dangerLevelId;
    private String dangerLevelName;
    private Long statusId;
    private String statusName;
    private LocalDateTime createdAt;
    private List<String> photoUrls;
}
