package com.geosegbar.infra.instrument.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.infra.historical_data_job.service.HistoricalDataJobService;
import com.geosegbar.infra.hydrotelemetric.services.AsyncHydrotelemetricCollectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinimetricRulerEventListener {

    private final AsyncHydrotelemetricCollectionService asyncCollectionService;
    private final HistoricalDataJobService historicalDataJobService;

    @EventListener
    public void handleLinimetricRulerCreated(LinimetricRulerCreatedEvent event) {
        log.info("Evento de criação/atualização de régua linimétrica recebido para instrumento ID: {}",
                event.getInstrument().getId());

        // Coleta instantânea (comportamento existente)
        asyncCollectionService.collectInstrumentDataAsync(event.getInstrument());

        // Coleta histórica (novo comportamento)
        try {
            if (!historicalDataJobService.hasActiveJobForInstrument(event.getInstrument().getId())) {
                HistoricalDataJobEntity job = historicalDataJobService.enqueueJob(
                        event.getInstrument().getId(),
                        event.getInstrument().getName()
                );
                log.info("Job de coleta histórica #{} enfileirado para o instrumento '{}' (ID: {})",
                        job.getId(),
                        event.getInstrument().getName(),
                        event.getInstrument().getId());
            } else {
                log.debug("Job de coleta histórica já existe para o instrumento '{}' (ID: {}). Pulando criação.",
                        event.getInstrument().getName(),
                        event.getInstrument().getId());
            }
        } catch (Exception e) {
            log.error("Erro ao criar job de coleta histórica para instrumento '{}' (ID: {}): {}",
                    event.getInstrument().getName(),
                    event.getInstrument().getId(),
                    e.getMessage(), e);
        }
    }
}
