package com.geosegbar.infra.checklist_response.services;

import java.util.Map;

import com.geosegbar.common.enums.AnomalyOriginEnum;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyPhotoEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.checklist_submission.dtos.OtherSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;

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
            AnomalyStatusEntity status,
            Long questionnaireId,
            Map<String, String> urlByObjectKey) {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setUser(checklistResponse.getUser());
        anomaly.setDam(checklistResponse.getDam());
        anomaly.setLatitude(other.getLatitude());
        anomaly.setLongitude(other.getLongitude());
        anomaly.setQuestionnaireId(questionnaireId);
        anomaly.setQuestionId(null);
        anomaly.setOrigin(AnomalyOriginEnum.CHECKLIST);
        anomaly.setObservation(other.getObservation());
        anomaly.setRecommendation(other.getRecommendation());
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);
        anomaly.setChecklistResponseId(checklistResponse.getId());
        anomaly.setCreatedAt(checklistResponse.getCreatedAt());

        attachPhotos(anomaly, other, urlByObjectKey);

        return anomaly;
    }

    /**
     * A API exige ao menos uma foto no apontamento, então perder a evidência em
     * silêncio seria pior do que recusar: a foto já subiu ao S3 e não há endpoint
     * para anexá-la a uma anomalia depois de criada.
     */
    private static void attachPhotos(AnomalyEntity anomaly, OtherSubmissionDTO other,
            Map<String, String> urlByObjectKey) {
        if (other.getPhotos() == null) {
            return;
        }

        for (PhotoSubmissionDTO photoDto : other.getPhotos()) {
            String objectKey = photoDto.getObjectKey();
            if (objectKey == null || objectKey.isBlank()) {
                throw new InvalidInputException(
                        "Toda foto deve conter 'objectKey' (chave S3 do upload pré-assinado).");
            }

            String url = urlByObjectKey.get(objectKey);
            if (url == null) {
                throw new InvalidInputException(
                        "Imagem não encontrada no S3 para a chave: " + objectKey);
            }

            AnomalyPhotoEntity photo = new AnomalyPhotoEntity();
            photo.setAnomaly(anomaly);
            photo.setImagePath(url);
            anomaly.getPhotos().add(photo);
        }
    }
}
