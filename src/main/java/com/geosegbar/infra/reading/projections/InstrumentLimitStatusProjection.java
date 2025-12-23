package com.geosegbar.infra.reading.projections;

import java.time.LocalDate;
import java.time.LocalTime;

public interface InstrumentLimitStatusProjection {

    Long getInstrumentId();

    String getInstrumentName();

    String getInstrumentTypeName();

    Long getInstrumentTypeId();

    Long getDamId();

    String getDamName();

    Long getClientId();

    String getClientName();

    LocalDate getReadingDate();

    LocalTime getReadingHour();

    String getLimitStatus();
}
