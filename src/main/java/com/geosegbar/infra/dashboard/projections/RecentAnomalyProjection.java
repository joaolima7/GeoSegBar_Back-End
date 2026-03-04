package com.geosegbar.infra.dashboard.projections;

import java.time.LocalDateTime;

public interface RecentAnomalyProjection {

    Long getId();

    Long getUserId();

    String getUserName();

    Long getDamId();

    String getDamName();

    LocalDateTime getCreatedAt();

    Double getLatitude();

    Double getLongitude();

    String getOrigin();

    String getObservation();

    String getRecommendation();

    String getDangerLevelName();

    String getStatusName();
}
