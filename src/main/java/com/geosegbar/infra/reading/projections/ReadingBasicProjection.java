package com.geosegbar.infra.reading.projections;

import java.time.LocalDate;
import java.time.LocalTime;

import com.geosegbar.common.enums.LimitStatusEnum;

public interface ReadingBasicProjection {

    Long getId();

    LocalDate getDate();

    LocalTime getHour();

    Double getCalculatedValue();

    LimitStatusEnum getLimitStatus();

    Boolean getActive();

    String getComment();

    Long getInstrumentId();

    String getInstrumentName();

    Long getOutputId();

    String getOutputName();

    String getOutputAcronym();

    Long getUserId();

    String getUserName();

    String getUserEmail();
}
