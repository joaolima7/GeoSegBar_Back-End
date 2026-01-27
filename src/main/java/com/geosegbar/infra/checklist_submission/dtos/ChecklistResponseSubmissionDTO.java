package com.geosegbar.infra.checklist_submission.dtos;

import java.util.List;

import com.geosegbar.common.enums.WeatherConditionEnum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistResponseSubmissionDTO {

    @NotNull(message = "ID da barragem é obrigatório!")
    @Positive(message = "ID da barragem deve ser um número positivo!")
    private Long damId;

    @NotBlank(message = "Nome do checklist é obrigatório!")
    private String checklistName;

    @NotNull(message = "ID do checklist é obrigatório!")
    @Positive(message = "ID do checklist deve ser um número positivo!")
    private Long checklistId;

    @NotNull(message = "ID do usuário é obrigatório!")
    @Positive(message = "ID do usuário deve ser um número positivo!")
    private Long userId;

    @NotEmpty(message = "É necessário incluir pelo menos um questionário!")
    @Valid
    private List<QuestionnaireResponseSubmissionDTO> questionnaireResponses;

    private boolean isMobile;

    @DecimalMin(value = "0.0", message = "Nível do montante deve ser maior ou igual a zero")
    @DecimalMax(value = "99999.99", message = "Nível do montante excede o valor máximo permitido")
    private Double upstreamLevel;

    @DecimalMin(value = "0.0", message = "Nível do jusante deve ser maior ou igual a zero")
    @DecimalMax(value = "99999.99", message = "Nível do jusante excede o valor máximo permitido")
    private Double downstreamLevel;

    @DecimalMin(value = "0.0", message = "Vazão vertida deve ser maior ou igual a zero")
    @DecimalMax(value = "99999.99", message = "Vazão vertida excede o valor máximo permitido")
    private Double spilledFlow;

    @DecimalMin(value = "0.0", message = "Vazão turbinada deve ser maior ou igual a zero")
    @DecimalMax(value = "99999.99", message = "Vazão turbinada excede o valor máximo permitido")
    private Double turbinedFlow;

    @DecimalMin(value = "0.0", message = "Pluviosidade acumulada deve ser maior ou igual a zero")
    @DecimalMax(value = "9999.99", message = "Pluviosidade acumulada excede o valor máximo permitido")
    private Double accumulatedRainfall;

    private WeatherConditionEnum weatherCondition;
}
