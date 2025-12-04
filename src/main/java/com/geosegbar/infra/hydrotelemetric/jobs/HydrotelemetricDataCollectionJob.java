package com.geosegbar.infra.hydrotelemetric.jobs;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.infra.hydrotelemetric.services.AnaApiService;
import com.geosegbar.infra.hydrotelemetric.services.HydrotelemetricCollectionService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class HydrotelemetricDataCollectionJob {

    private final AnaApiService anaApiService;
    private final InstrumentRepository instrumentRepository;
    private final HydrotelemetricCollectionService collectionService;

    @Scheduled(cron = "0 30 0 * * ?")
    public void collectHydrotelemetricData() {
        log.info("Iniciando coleta de dados hidrotelemétricos");

        try {
            String authToken = anaApiService.getAuthToken();

            List<InstrumentEntity> linimetricInstruments = instrumentRepository.findByIsLinimetricRulerTrue();
            log.info("Encontrados {} instrumentos do tipo régua linimétrica", linimetricInstruments.size());

            LocalDate today = LocalDate.now();

            for (InstrumentEntity instrument : linimetricInstruments) {
                try {

                    collectionService.collectInstrumentData(instrument, authToken, today, null);
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

    @Transactional
    public void collectDataManually() {
        collectHydrotelemetricData();
    }
}
