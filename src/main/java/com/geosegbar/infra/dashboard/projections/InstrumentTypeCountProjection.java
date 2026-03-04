package com.geosegbar.infra.dashboard.projections;

public interface InstrumentTypeCountProjection {

    Long getTypeId();

    String getTypeName();

    Long getTotal();
}
