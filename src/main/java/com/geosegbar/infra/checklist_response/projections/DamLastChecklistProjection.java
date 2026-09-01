package com.geosegbar.infra.checklist_response.projections;

import java.time.LocalDateTime;

public interface DamLastChecklistProjection {

    Long getDamId();

    String getDamName();

    LocalDateTime getLastChecklistDate();
}
