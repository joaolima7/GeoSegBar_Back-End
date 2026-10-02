package com.geosegbar.infra.checklist_response.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.checklist_response.dtos.AnswerUpdateDTO;
import com.geosegbar.infra.checklist_response.dtos.ChecklistResponseUpdateDTO;
import com.geosegbar.infra.checklist_submission.dtos.OtherSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;
import com.geosegbar.infra.checklist_submission.services.ChecklistPhotoPresignService;

import lombok.RequiredArgsConstructor;

/**
 * Contraparte do submit-presigned para a EDIÇÃO: as fotos já foram enviadas ao
 * S3 e chegam por {@code objectKey}. Valida as chaves, reconstrói as URLs no
 * servidor e só então persiste.
 */
@Service
@RequiredArgsConstructor
public class ChecklistResponseUpdatePresignedService {

    private final ChecklistResponseService checklistResponseService;
    private final ChecklistPhotoPresignService presignService;

    public void updateChecklistResponse(Long checklistResponseId, ChecklistResponseUpdateDTO dto) {
        List<PhotoSubmissionDTO> photos = collectPhotos(dto);

        for (PhotoSubmissionDTO photo : photos) {
            String key = photo.getObjectKey();
            if (key == null || key.isBlank()) {
                throw new InvalidInputException(
                        "Toda foto deve conter 'objectKey' (chave S3 do upload pré-assinado).");
            }
            if (!presignService.isAllowedKey(key)) {
                throw new InvalidInputException("objectKey inválido (prefixo não permitido): " + key);
            }
        }

        List<String> distinctKeys = photos.stream()
                .map(PhotoSubmissionDTO::getObjectKey)
                .distinct()
                .toList();

        List<String> missing = distinctKeys.stream()
                .filter(key -> !presignService.objectExists(key))
                .toList();
        if (!missing.isEmpty()) {
            throw new InvalidInputException(
                    "Imagens não encontradas no S3. Envie todas as imagens (PUT) antes de salvar. "
                    + "Chaves ausentes: " + missing);
        }

        Map<String, String> urlByObjectKey = new HashMap<>();
        for (String key : distinctKeys) {
            urlByObjectKey.put(key, presignService.publicUrl(key));
        }

        checklistResponseService.updateChecklistResponse(checklistResponseId, dto, urlByObjectKey);

        presignService.confirmKeys(distinctKeys);
    }

    private List<PhotoSubmissionDTO> collectPhotos(ChecklistResponseUpdateDTO dto) {
        List<PhotoSubmissionDTO> photos = new ArrayList<>();

        if (dto.getAnswers() != null) {
            for (AnswerUpdateDTO answer : dto.getAnswers()) {
                if (answer.getPhotos() != null) {
                    photos.addAll(answer.getPhotos());
                }
            }
        }

        if (dto.getOthers() != null) {
            for (OtherSubmissionDTO other : dto.getOthers()) {
                if (other.getPhotos() != null) {
                    photos.addAll(other.getPhotos());
                }
            }
        }

        return photos;
    }
}
