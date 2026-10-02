package com.geosegbar.infra.checklist_response.services;

import java.util.Comparator;
import java.util.List;

import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyPhotoEntity;
import com.geosegbar.infra.checklist_response.dtos.ChecklistAnomalyDTO;

public final class ChecklistAnomalyMapper {

    private ChecklistAnomalyMapper() {
    }

    public static ChecklistAnomalyDTO toDto(AnomalyEntity anomaly) {
        List<String> photoUrls = anomaly.getPhotos() == null
                ? List.of()
                : anomaly.getPhotos().stream()
                        .sorted(Comparator.comparing(AnomalyPhotoEntity::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(AnomalyPhotoEntity::getImagePath)
                        .filter(path -> path != null && !path.isBlank())
                        .toList();

        return new ChecklistAnomalyDTO(
                anomaly.getId(),
                anomaly.getQuestionId(),
                anomaly.getQuestionnaireId(),
                anomaly.getObservation(),
                anomaly.getRecommendation(),
                anomaly.getLatitude(),
                anomaly.getLongitude(),
                anomaly.getDangerLevel() != null ? anomaly.getDangerLevel().getId() : null,
                anomaly.getDangerLevel() != null ? anomaly.getDangerLevel().getName() : null,
                anomaly.getStatus() != null ? anomaly.getStatus().getId() : null,
                anomaly.getStatus() != null ? anomaly.getStatus().getName() : null,
                anomaly.getCreatedAt(),
                photoUrls);
    }
}
