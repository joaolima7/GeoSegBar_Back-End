package com.geosegbar.infra.checklist_submission.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistPhotoPresignRequestDTO;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistPhotoPresignResponseDTO;
import com.geosegbar.infra.checklist_submission.services.ChecklistPhotoPresignService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * FASE 1 do fluxo presigned — gera URLs pré-assinadas para o front subir as
 * fotos do checklist direto ao S3 (sem passar pelo backend).
 */
@RestController
@RequestMapping("/checklist-responses/photos/presign")
@RequiredArgsConstructor
public class ChecklistPhotoPresignController {

    private final ChecklistPhotoPresignService presignService;

    @PostMapping
    public ResponseEntity<WebResponseEntity<ChecklistPhotoPresignResponseDTO>> presign(
            @Valid @RequestBody ChecklistPhotoPresignRequestDTO request) {

        ChecklistPhotoPresignResponseDTO result = presignService.presignBatch(request);

        WebResponseEntity<ChecklistPhotoPresignResponseDTO> response = WebResponseEntity.success(
                result,
                "URLs de upload geradas com sucesso!"
        );

        return ResponseEntity.ok(response);
    }
}
