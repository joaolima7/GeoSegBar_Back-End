package com.geosegbar.infra.reading.projections;

import java.sql.Date;
import java.sql.Time;

/**
 * ⭐ Projection para buscar últimas readings com limit status por instrumento
 * Usado na query otimizada com Window Function
 */
public interface InstrumentLatestReadingProjection {

    Long getInstrumentId();

    String getInstrumentName();

    String getInstrumentTypeName();

    Long getDamId();

    String getDamName();

    Long getClientId();

    String getClientName();

    Date getDate();

    Time getHour();

    String getLimitStatus();
}
