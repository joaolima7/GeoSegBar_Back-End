package com.geosegbar.infra.checklist_response.services;

import com.geosegbar.common.enums.AnomalyOriginEnum;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.infra.checklist_submission.dtos.OtherSubmissionDTO;

/**
 * Apontamento de "Outros" registrado durante a edição de um checklist já
 * preenchido. Nasce com a data da inspeção, não a da edição, para ficar na mesma
 * linha do tempo dos apontamentos feitos em campo.
 */
public final class EditedOtherAnomalyFactory {

    private EditedOtherAnomalyFactory() {
    }

    public static AnomalyEntity build(OtherSubmissionDTO other,
            ChecklistResponseEntity checklistResponse,
            DangerLevelEntity dangerLevel,
            AnomalyStatusEntity status) {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setUser(checklistResponse.getUser());
        anomaly.setDam(checklistResponse.getDam());
        anomaly.setLatitude(other.getLatitude());
        anomaly.setLongitude(other.getLongitude());
        anomaly.setQuestionnaireId(null);
        anomaly.setQuestionId(null);
        anomaly.setOrigin(AnomalyOriginEnum.CHECKLIST);
        anomaly.setObservation(other.getObservation());
        anomaly.setRecommendation(other.getRecommendation());
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);
        anomaly.setChecklistResponseId(checklistResponse.getId());
        anomaly.setCreatedAt(checklistResponse.getCreatedAt());
        return anomaly;
    }
}
