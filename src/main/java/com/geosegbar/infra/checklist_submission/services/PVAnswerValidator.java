package com.geosegbar.infra.checklist_submission.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.geosegbar.entities.OptionEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PVAnswerValidator {

    private final OptionRepository optionRepository;

    public void validatePVAnswer(AnswerSubmissionDTO answerDto, String questionText) {
        boolean isPVAnswer = false;

        if (answerDto.getSelectedOptionIds() != null && !answerDto.getSelectedOptionIds().isEmpty()) {
            for (Long optionId : answerDto.getSelectedOptionIds()) {
                OptionEntity option = optionRepository.findById(optionId)
                        .orElseThrow(() -> new NotFoundException("Opção não encontrada: " + optionId));

                if ("PV".equals(option.getLabel())) {
                    isPVAnswer = true;
                    break;
                }
            }
        }

        if (isPVAnswer) {
            List<String> missingFields = new ArrayList<>();

            if (answerDto.getAnomalyRecommendation() == null || answerDto.getAnomalyRecommendation().trim().isEmpty()) {
                missingFields.add("recommendation");
            }

            if (answerDto.getAnomalyDangerLevelId() == null) {
                missingFields.add("dangerLevelId");
            }

            if (answerDto.getAnomalyStatusId() == null) {
                missingFields.add("statusId");
            }

            if (answerDto.getPhotos() == null || answerDto.getPhotos().isEmpty()) {
                missingFields.add("photo");
            }

            if (answerDto.getLatitude() == null || answerDto.getLongitude() == null) {
                missingFields.add("latitude/longitude");
            }

            if (!missingFields.isEmpty()) {
                throw new InvalidInputException("A pergunta '" + questionText
                        + "' foi marcada como PV, mas faltam os seguintes campos obrigatórios para criar a anomalia: "
                        + String.join(", ", missingFields));
            }
        }
    }

    public boolean isPVAnswer(AnswerSubmissionDTO answerDto) {
        if (answerDto.getSelectedOptionIds() != null && !answerDto.getSelectedOptionIds().isEmpty()) {
            for (Long optionId : answerDto.getSelectedOptionIds()) {
                OptionEntity option = optionRepository.findById(optionId)
                        .orElseThrow(() -> new NotFoundException("Opção não encontrada: " + optionId));

                if ("PV".equals(option.getLabel())) {
                    return true;
                }
            }
        }
        return false;
    }
}
