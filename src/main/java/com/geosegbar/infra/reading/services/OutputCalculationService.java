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

    public Double calculateOutput(OutputEntity output, ReadingRequestDTO reading, Map<String, Double> inputValues) {
        InstrumentEntity instrument = output.getInstrument();

        // Copia os valores de input para o mapa de variáveis
        Map<String, Double> variables = new HashMap<>(inputValues);

        // Adiciona os valores das constantes
        for (ConstantEntity constant : instrument.getConstants()) {
            variables.put(constant.getAcronym(), constant.getValue());
        }

        // Realizar o cálculo usando o ExpressionEvaluator
        Double result = ExpressionEvaluator.evaluate(output.getEquation(), variables);

        // Aplicar a precisão definida no output
        return formatToSpecificPrecision(result, output.getPrecision());
    }

// Método calculateAllOutputs também precisa ser atualizado para receber Map<String, Double> diretamente
    public Map<String, Double> calculateAllOutputs(InstrumentEntity instrument, ReadingRequestDTO reading,
            Map<String, Double> inputValues) {
        Map<String, Double> results = new HashMap<>();

        for (OutputEntity output : instrument.getOutputs()) {
            Double value = calculateOutput(output, reading, inputValues);
            results.put(output.getAcronym(), value);
        }

        return results;
    }

    private Double formatToSpecificPrecision(Double value, Integer precision) {
        if (value == null || precision == null) {
            return value;
        }

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
