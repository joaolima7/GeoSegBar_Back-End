package com.geosegbar.infra.instrument.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.geosegbar.infra.hydrotelemetric.services.AsyncHydrotelemetricCollectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinimetricRulerEventListener {

    private final AsyncHydrotelemetricCollectionService asyncCollectionService;

    @EventListener
    public void handleLinimetricRulerCreated(LinimetricRulerCreatedEvent event) {
        log.info("Evento de criação/atualização de régua linimétrica recebido para instrumento ID: {}",
                event.getInstrument().getId());

        asyncCollectionService.collectInstrumentDataAsync(event.getInstrument());
    }
}
