package com.geosegbar.infra.reading.projections;

import java.time.LocalDate;
import java.time.LocalTime;

import com.geosegbar.common.enums.LimitStatusEnum;

/**
 * Projection para buscar dados básicos de reading sem carregar entidades
 * completas
 */
public interface ReadingBasicProjection {

    Long getId();

    LocalDate getDate();

    LocalTime getHour();

    Double getCalculatedValue();

    LimitStatusEnum getLimitStatus();

    Boolean getActive();

    String getComment();

    // Instrument info
    Long getInstrumentId();

    String getInstrumentName();

    // Output info
    Long getOutputId();

    String getOutputName();

    String getOutputAcronym();

    // User info
    Long getUserId();

    String getUserName();

    String getUserEmail();
}
