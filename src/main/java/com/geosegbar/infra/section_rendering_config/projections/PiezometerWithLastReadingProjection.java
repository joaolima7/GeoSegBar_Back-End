package com.geosegbar.infra.section_rendering_config.projections;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public interface PiezometerWithLastReadingProjection {
    Long getId();
    String getName();
    Double getDistanceOffset();
    LocalDate getLastReadingDate();
    LocalTime getLastReadingHour();
    BigDecimal getLastReadingValue();
    String getLastReadingLimitStatus();
}
