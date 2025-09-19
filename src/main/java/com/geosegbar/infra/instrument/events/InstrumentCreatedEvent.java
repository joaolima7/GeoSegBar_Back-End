package com.geosegbar.infra.instrument.events;

import com.geosegbar.entities.InstrumentEntity;

import lombok.Getter;

@Getter
public class InstrumentCreatedEvent {

    private final InstrumentEntity instrument;

    public InstrumentCreatedEvent(InstrumentEntity instrument) {
        this.instrument = instrument;
    }
}
