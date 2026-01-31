package com.geosegbar.infra.checklist_submission.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PVAnswerValidator {

    public void validatePVAnswer(AnswerSubmissionDTO answerDto, String questionText, Map<Long, String> optionsCache) {
        boolean isPVAnswer = isPVAnswer(answerDto, optionsCache);

        if (isPVAnswer) {
            List<String> missingFields = new ArrayList<>();

            if (answerDto.getAnomalyRecommendation() == null || answerDto.getAnomalyRecommendation().trim().isEmpty()) {
                missingFields.add("Recomendação");
            }

            if (answerDto.getAnomalyDangerLevelId() == null) {
                missingFields.add("Nível de Perigo");
            }

            if (answerDto.getAnomalyStatusId() == null) {
                missingFields.add("Status");
            }

            if (answerDto.getPhotos() == null || answerDto.getPhotos().isEmpty()) {
                missingFields.add("Foto");
            }

            if (answerDto.getLatitude() == null || answerDto.getLongitude() == null) {
                missingFields.add("Localização (Lat/Long)");
            }

            if (!missingFields.isEmpty()) {
                throw new InvalidInputException("A pergunta '" + questionText
                        + "' foi marcada como PV (Patologia), mas faltam campos obrigatórios: "
                        + String.join(", ", missingFields));
            }
        }
    }

    public boolean isPVAnswer(AnswerSubmissionDTO answerDto, Map<Long, String> optionsCache) {
        if (answerDto.getSelectedOptionIds() != null && !answerDto.getSelectedOptionIds().isEmpty()) {
            for (Long optionId : answerDto.getSelectedOptionIds()) {

                String label = optionsCache.get(optionId);

                if ("PV".equals(label)) {
                    return true;
                }
            }
        }
        return false;
    }
}
