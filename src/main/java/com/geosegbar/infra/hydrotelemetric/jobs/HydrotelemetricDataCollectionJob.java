package com.geosegbar.infra.hydrotelemetric.jobs;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.response.AnaTelemetryResponse.TelemetryItem;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.infra.hydrotelemetric.services.AnaApiService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.services.ReadingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class HydrotelemetricDataCollectionJob {

    private final AnaApiService anaApiService;
    private final InstrumentRepository instrumentRepository;
    private final ReadingService readingService;

    @Scheduled(cron = "0 36 11 * * ?")
    public void collectHydrotelemetricData() {
        log.info("Iniciando coleta de dados hidrotelemétricos");

        try {
            String authToken = anaApiService.getAuthToken();

            List<InstrumentEntity> linimetricInstruments = instrumentRepository.findByIsLinimetricRulerTrue();
            log.info("Encontrados {} instrumentos do tipo régua linimétrica", linimetricInstruments.size());

            LocalDate today = LocalDate.now();

            for (InstrumentEntity instrument : linimetricInstruments) {
                try {
                    collectInstrumentData(instrument, authToken, today);
                } catch (Exception e) {
                    log.error("Erro ao coletar dados para o instrumento {}: {}",
                            instrument.getName(), e.getMessage(), e);
                }
            }

            log.info("Coleta de dados hidrotelemétricos finalizada com sucesso");
        } catch (Exception e) {
            log.error("Erro durante a coleta de dados hidrotelemétricos: {}", e.getMessage(), e);
        }
    }

    @Transactional(timeout = 60)
    private void collectInstrumentData(InstrumentEntity instrument, String authToken, LocalDate date) {
        Long linimetricCode = instrument.getLinimetricRulerCode();

        // Verificar se o instrumento tem código de régua linimétrica
        if (linimetricCode == null) {
            log.warn("Instrumento {} não possui código de régua linimétrica. Ignorando.",
                    instrument.getName());
            return;
        }

        // Verificar se já existe leitura para este instrumento nesta data
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
            Double averageM = averageMm != null ? averageMm / 1000.0 : null;

            // Só prosseguir se tivermos um valor válido para registrar
            if (averageM == null) {
                log.warn("Valor nulo obtido para instrumento: {}. Nenhuma leitura será registrada.",
                        instrument.getName());
                return;
            }

            // Obter o input principal do instrumento linimétrico (deve ser o input "LEI")
            String inputAcronym = "LEI";

            // Verificar se o instrumento possui este input
            boolean hasRequiredInput = instrument.getInputs().stream()
                    .anyMatch(input -> input.getAcronym().equals(inputAcronym));

            if (!hasRequiredInput) {
                log.error("Instrumento {} não possui o input necessário '{}'. Ignorando.",
                        instrument.getName(), inputAcronym);
                return;
            }

            // Criar o objeto de requisição de leitura
            ReadingRequestDTO readingRequest = new ReadingRequestDTO();
            readingRequest.setDate(date);
            readingRequest.setHour(LocalTime.of(0, 30));  // Meia-noite como padrão

            Map<String, Double> inputValues = new HashMap<>();
            inputValues.put(inputAcronym, averageM);
            readingRequest.setInputValues(inputValues);

            readingRequest.setComment("Leitura automática da API ANA");

            // Registrar a leitura usando o ReadingService existente
            readingService.create(instrument.getId(), readingRequest);

            log.info("Leitura linimétrica registrada com sucesso para instrumento: {} - Valor: {}m",
                    instrument.getName(), averageM);

        } catch (Exception e) {
            log.error("Erro ao coletar dados para instrumento {}: {}",
                    instrument.getName(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void collectDataManually() {
        collectHydrotelemetricData();
    }
}
