package com.geosegbar.infra.reading.projections;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ⭐ Projection minimalista para buscar apenas dados necessários para limit
 * status
 */
public interface ReadingWithLimitStatusProjection {

    Long getId();

    LocalDate getDate();

    LocalTime getHour();

    String getLimitStatus();

    Double getCalculatedValue();
}
