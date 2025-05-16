package com.geosegbar.infra.checklist_submission.dtos;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmissionDTO {

    @NotNull(message = "ID da pergunta é obrigatório!")
    @Positive(message = "ID da pergunta deve ser um número positivo!")
    private Long questionId;

    private String comment;

    private Double latitude;

    private Double longitude;

    private List<Long> selectedOptionIds;

    private List<PhotoSubmissionDTO> photos;

    private String anomalyRecommendation;
    private Long anomalyDangerLevelId;
    private Long anomalyStatusId;
}
