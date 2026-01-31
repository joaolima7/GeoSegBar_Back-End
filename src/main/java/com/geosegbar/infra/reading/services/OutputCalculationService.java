package com.geosegbar.infra.reading.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.geosegbar.common.utils.ExpressionEvaluator;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutputCalculationService {

    public BigDecimal calculateOutput(OutputEntity output, ReadingRequestDTO reading, Map<String, BigDecimal> inputValues) {
        InstrumentEntity instrument = output.getInstrument();

        Map<String, Double> variables = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : inputValues.entrySet()) {
            variables.put(entry.getKey(), entry.getValue().doubleValue());
        }

        for (ConstantEntity constant : instrument.getConstants()) {
            variables.put(constant.getAcronym(), constant.getValue());
        }

        Double rawResult = ExpressionEvaluator.evaluate(output.getEquation(), variables);

        return formatToSpecificPrecision(rawResult, output.getPrecision());
    }

    public Map<String, BigDecimal> calculateAllOutputs(InstrumentEntity instrument, ReadingRequestDTO reading,
            Map<String, BigDecimal> inputValues) {
        Map<String, BigDecimal> results = new HashMap<>();

        for (OutputEntity output : instrument.getOutputs()) {
            BigDecimal value = calculateOutput(output, reading, inputValues);
            results.put(output.getAcronym(), value);
        }

        return results;
    }

    private BigDecimal formatToSpecificPrecision(Double value, Integer precision) {
        if (value == null) {
            return null;
        }

        BigDecimal bd = BigDecimal.valueOf(value);

        if (precision != null) {
            bd = bd.setScale(precision, RoundingMode.HALF_UP);
        }

        return bd;
    }
}
