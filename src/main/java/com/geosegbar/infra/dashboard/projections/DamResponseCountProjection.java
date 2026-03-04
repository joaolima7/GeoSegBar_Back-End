package com.geosegbar.infra.dashboard.projections;

public interface DamResponseCountProjection {

    Long getDamId();

    String getDamName();

    Long getTotal();
}
