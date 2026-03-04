package com.geosegbar.infra.dashboard.projections;

public interface ChecklistResponseCountProjection {

    Long getChecklistId();

    String getChecklistName();

    Long getTotal();
}
