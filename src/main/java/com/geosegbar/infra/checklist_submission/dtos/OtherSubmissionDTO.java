package com.geosegbar.infra.checklist_submission.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtherSubmissionDTO {

    @NotBlank(message = "Observation is required for 'others' entries!")
    private String observation;

    private String recommendation;

    @NotNull(message = "Danger level ID is required for 'others' entries!")
    private Long anomalyDangerLevelId;

    @NotNull(message = "Status ID is required for 'others' entries!")
    private Long anomalyStatusId;

    @NotNull(message = "Latitude is required for 'others' entries!")
    private Double latitude;

    @NotNull(message = "Longitude is required for 'others' entries!")
    private Double longitude;

    @NotNull(message = "At least one photo is required for 'others' entries!")
    @Size(min = 1, message = "At least one photo is required for 'others' entries!")
    @Valid
    private List<PhotoSubmissionDTO> photos;
}
