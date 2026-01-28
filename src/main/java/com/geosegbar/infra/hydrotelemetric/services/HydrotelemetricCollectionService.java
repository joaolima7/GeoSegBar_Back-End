package com.geosegbar.infra.hydrotelemetric.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.response.AnaTelemetryResponse.TelemetryItem;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.services.ReadingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HydrotelemetricCollectionService {

    private final AnaApiService anaApiService;
    private final ReadingService readingService;

    @Transactional(timeout = 60)
    public void collectInstrumentData(InstrumentEntity instrument, String authToken, LocalDate date, String commentSuffix) {
        Long linimetricCode = instrument.getLinimetricRulerCode();

        if (linimetricCode == null) {
            log.warn("Instrumento {} não possui código de régua linimétrica. Ignorando.",
                    instrument.getName());
            return;
        }

        boolean readingExists = readingService.existsByInstrumentAndDate(instrument.getId(), date);
        if (readingExists) {
            log.info("Já existe leitura para o instrumento {} na data {}. Ignorando.",
                    instrument.getName(), date);
            return;
        }

        try {
            String stationCode = String.valueOf(linimetricCode);
            List<TelemetryItem> telemetryData = anaApiService.getTelemetryData(stationCode, authToken);

            Double averageMm = anaApiService.calculateAverageLevel(telemetryData, date);

            if (averageMm == null) {
                log.warn("Valor nulo obtido para instrumento: {}. Nenhuma leitura será registrada.",
                        instrument.getName());
                return;
            }

            String inputAcronym = "LEI";

            boolean hasRequiredInput = instrument.getInputs().stream()
                    .anyMatch(input -> input.getAcronym().equals(inputAcronym));

            if (!hasRequiredInput) {
                log.error("Instrumento {} não possui o input necessário '{}'. Ignorando.",
                        instrument.getName(), inputAcronym);
                return;
            }

            ReadingRequestDTO readingRequest = new ReadingRequestDTO();
            readingRequest.setDate(date);
            readingRequest.setHour(LocalTime.of(0, 30));

            Map<String, Double> inputValues = new HashMap<>();
            inputValues.put(inputAcronym, averageMm);
            readingRequest.setInputValues(inputValues);

            String baseComment = "Leitura automática pela ANA";
            readingRequest.setComment(commentSuffix != null ? baseComment + " " + commentSuffix : baseComment + ".");

            readingService.create(instrument.getId(), readingRequest, true);

            log.info("Leitura linimétrica registrada com sucesso para instrumento: {} - Valor: {} mm",
                    instrument.getName(), averageMm);

        } catch (Exception e) {
            log.error("Erro ao coletar dados para instrumento {}: {}",
                    instrument.getName(), e.getMessage());
            throw e;
        }
    }
}
