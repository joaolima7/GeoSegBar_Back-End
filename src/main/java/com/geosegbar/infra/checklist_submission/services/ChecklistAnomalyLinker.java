package com.geosegbar.infra.checklist_submission.services;

import java.time.LocalDateTime;

import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.ChecklistResponseEntity;

/**
 * A anomalia nascida de um checklist herda a data da inspeção, não o instante
 * em que foi gravada. É isso que faz um checklist lançado com data retroativa
 * produzir anomalias com a data em que a inspeção realmente aconteceu.
 */
public final class ChecklistAnomalyLinker {

    private ChecklistAnomalyLinker() {
    }

    public static void link(AnomalyEntity anomaly, ChecklistResponseEntity checklistResponse) {
        anomaly.setChecklistResponseId(checklistResponse.getId());
        anomaly.setCreatedAt(checklistResponse.getCreatedAt() != null
                ? checklistResponse.getCreatedAt()
                : LocalDateTime.now());
    }
}
