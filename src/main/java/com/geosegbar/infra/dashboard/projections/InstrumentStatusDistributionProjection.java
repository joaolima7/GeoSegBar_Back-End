package com.geosegbar.infra.dashboard.projections;

public interface InstrumentStatusDistributionProjection {

    Long getTypeId();

    String getTypeName();

    String getLimitStatus();

    Long getTotal();
}
