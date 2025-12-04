package com.geosegbar.infra.instrument.events;

import org.springframework.context.ApplicationEvent;

import com.geosegbar.entities.InstrumentEntity;

import lombok.Getter;

@Getter
public class LinimetricRulerCreatedEvent extends ApplicationEvent {

    private final InstrumentEntity instrument;

    public LinimetricRulerCreatedEvent(Object source, InstrumentEntity instrument) {
        super(source);
        this.instrument = instrument;
    }
}
