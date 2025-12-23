package com.geosegbar.infra.reading.projections;

import java.sql.Date;
import java.sql.Time;

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
