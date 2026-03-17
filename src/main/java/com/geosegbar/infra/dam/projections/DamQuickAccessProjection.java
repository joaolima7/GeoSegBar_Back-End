package com.geosegbar.infra.dam.projections;

public interface DamQuickAccessProjection {

    Long getDamId();

    String getDamName();

    String getStatus();

    Long getClientId();

    String getClientName();
}
