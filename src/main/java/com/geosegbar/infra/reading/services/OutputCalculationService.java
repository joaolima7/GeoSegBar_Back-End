package com.geosegbar.infra.reading.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.infra.math.ExpressionEvaluator;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutputCalculationService {

// Atualize o método calculateOutput
    public Double calculateOutput(OutputEntity output, ReadingRequestDTO reading, Map<String, Double> inputValues) {
        InstrumentEntity instrument = output.getInstrument();

        // Copia os valores de input para o mapa de variáveis
        Map<String, Double> variables = new HashMap<>(inputValues);

        // Adiciona os valores das constantes
        for (ConstantEntity constant : instrument.getConstants()) {
            variables.put(constant.getAcronym(), constant.getValue());
        }

        return ExpressionEvaluator.evaluate(output.getEquation(), variables);
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
}
