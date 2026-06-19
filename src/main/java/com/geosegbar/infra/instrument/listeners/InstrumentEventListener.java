package com.geosegbar.infra.instrument.listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.geosegbar.infra.instrument.events.InstrumentCreatedEvent;
import com.geosegbar.infra.instrument.services.AutoPatternCreationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstrumentEventListener {

    private final AutoPatternCreationService autoPatternCreationService;

    /**
     * Dispara a criação automática de padrões SOMENTE após o commit da transação
     * que criou o instrumento. Antes (com @EventListener síncrono) o método
     * assíncrono podia rodar antes do commit e não encontrar o instrumento no
     * banco (race condition: "uns dão certo, outros não").
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInstrumentCreatedEvent(InstrumentCreatedEvent event) {
        autoPatternCreationService.createPatternsForInstrument(event.getInstrument());
    }
}
