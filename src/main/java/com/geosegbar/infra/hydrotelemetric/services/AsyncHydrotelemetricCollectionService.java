package com.geosegbar.infra.hydrotelemetric.services;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.InstrumentEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncHydrotelemetricCollectionService {

    private final AnaApiService anaApiService;
    private final HydrotelemetricCollectionService collectionService;

    @Async("hydrotelemetricTaskExecutor")
    public void collectInstrumentDataAsync(InstrumentEntity instrument) {
        log.info("Iniciando coleta assíncrona de dados hidrotelemétricos para instrumento: {} (ID: {})",
                instrument.getName(), instrument.getId());

        try {
            String authToken = anaApiService.getAuthToken();
            LocalDate today = LocalDate.now();

            collectionService.collectInstrumentData(
                    instrument,
                    authToken,
                    today,
                    "após criação/atualização do instrumento."
            );

            log.info("Coleta assíncrona finalizada para instrumento: {}", instrument.getName());

        } catch (Exception e) {
            log.error("Erro na coleta assíncrona de dados para instrumento {}: {}",
                    instrument.getName(), e.getMessage(), e);

        }
    }
}
