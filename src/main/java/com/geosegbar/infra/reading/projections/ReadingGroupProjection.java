package com.geosegbar.infra.reading.projections;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.geosegbar.common.enums.LimitStatusEnum;

public interface ReadingGroupProjection {

    Long getId();

    LocalDate getDate();

    LocalTime getHour();

    BigDecimal getCalculatedValue();

    LimitStatusEnum getLimitStatus();

    String getComment();

    Boolean getActive();

    Long getOutputId();

    String getOutputName();

    String getOutputAcronym();

    Long getUserId();

    String getUserName();
}
