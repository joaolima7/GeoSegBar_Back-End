package com.geosegbar.infra.section_rendering_config.projections;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public interface TelemetricInstrumentProjection {
    Long getId();
    String getName();
    Boolean getIsLinimetricRuler();
    Boolean getIsDownstream();
    LocalDate getLastReadingDate();
    LocalTime getLastReadingHour();
    BigDecimal getLastReadingValue();
    String getLastReadingLimitStatus();
}
