package com.geosegbar.infra.hydrotelemetric.jobs;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.infra.audit.services.AuditService;
import com.geosegbar.infra.hydrotelemetric.services.AnaApiService;
import com.geosegbar.infra.hydrotelemetric.services.HydrotelemetricCollectionService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class HydrotelemetricDataCollectionJob {

    private static final String ACTION = "JOB_HYDROTELEMETRIC_COLLECTION";
    private static final String ACTION_LABEL = "Coleta de dados hidrotelemétricos";

    private final AnaApiService anaApiService;
    private final InstrumentRepository instrumentRepository;
    private final HydrotelemetricCollectionService collectionService;
    private final AuditService auditService;

    @Scheduled(cron = "0 30 0 * * ?")
    public void collectHydrotelemetricData() {
        log.info("Iniciando coleta de dados hidrotelemétricos");
        long start = System.nanoTime();
        String traceId = auditService.newTraceId();

        try {
            String authToken = anaApiService.getAuthToken();

            List<InstrumentEntity> linimetricInstruments = instrumentRepository.findAllActiveWithLinimetricRulerCode();
            log.info("Encontrados {} instrumentos ativos com código ANA", linimetricInstruments.size());

            LocalDate today = LocalDate.now();

            int success = 0;
            int failures = 0;
            for (InstrumentEntity instrument : linimetricInstruments) {
                try {

                    collectionService.collectInstrumentData(instrument, authToken, today, null);
                    success++;
                } catch (Exception e) {
                    failures++;
                    log.error("Erro ao coletar dados para o instrumento {}: {}",
                            instrument.getName(), e.getMessage(), e);
                }
            }

            log.info("Coleta de dados hidrotelemétricos finalizada com sucesso");
            auditService.recordJobSuccess(ACTION, ACTION_LABEL, AuditSource.SCHEDULED,
                    "Coleta concluída: " + success + " instrumento(s) com sucesso, "
                    + failures + " com falha (de " + linimetricInstruments.size() + ").",
                    traceId, durationMs(start));
        } catch (Exception e) {
            log.error("Erro durante a coleta de dados hidrotelemétricos: {}", e.getMessage(), e);
            auditService.recordJobError(ACTION, ACTION_LABEL, AuditSource.SCHEDULED,
                    "Falha geral na coleta de dados hidrotelemétricos.", e, traceId, durationMs(start));
        }
    }

    private long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    @Transactional
    public void collectDataManually() {
        collectHydrotelemetricData();
    }
}
