package com.geosegbar.infra.checklist_response.dtos;

import java.util.List;

import com.geosegbar.common.enums.WeatherConditionEnum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistResponseUpdateDTO {

    @DecimalMin(value = "0.0", message = "Nível do montante deve ser maior ou igual a zero!")
    @DecimalMax(value = "99999.99", message = "Nível do montante excede o valor máximo permitido!")
    private Double upstreamLevel;

    @DecimalMin(value = "0.0", message = "Nível do jusante deve ser maior ou igual a zero!")
    @DecimalMax(value = "99999.99", message = "Nível do jusante excede o valor máximo permitido!")
    private Double downstreamLevel;

    @DecimalMin(value = "0.0", message = "Vazão vertida deve ser maior ou igual a zero!")
    @DecimalMax(value = "99999.99", message = "Vazão vertida excede o valor máximo permitido!")
    private Double spilledFlow;

    @DecimalMin(value = "0.0", message = "Vazão turbinada deve ser maior ou igual a zero!")
    @DecimalMax(value = "99999.99", message = "Vazão turbinada excede o valor máximo permitido!")
    private Double turbinedFlow;

    @DecimalMin(value = "0.0", message = "Pluviosidade acumulada deve ser maior ou igual a zero!")
    @DecimalMax(value = "9999.99", message = "Pluviosidade acumulada excede o valor máximo permitido!")
    private Double accumulatedRainfall;

    private WeatherConditionEnum weatherCondition;

    @Valid
    private List<AnswerUpdateDTO> answers;
}
