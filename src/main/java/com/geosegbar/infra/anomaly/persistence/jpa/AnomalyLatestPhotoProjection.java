package com.geosegbar.infra.anomaly.persistence.jpa;

import java.time.LocalDateTime;

public interface AnomalyLatestPhotoProjection {

    Long getAnomalyId();

    Long getChecklistResponseId();

    LocalDateTime getPhotoDate();

    String getImagePath();
}
