package com.geosegbar.infra.instrument.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.geosegbar.infra.instrument.events.InstrumentCreatedEvent;
import com.geosegbar.infra.instrument.services.AutoPatternCreationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstrumentEventListener {

    private final AutoPatternCreationService autoPatternCreationService;

    @EventListener
    public void handleInstrumentCreatedEvent(InstrumentCreatedEvent event) {
        autoPatternCreationService.createPatternsForInstrument(event.getInstrument());
    }
}
