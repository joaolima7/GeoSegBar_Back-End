package com.geosegbar.infra.checklist_submission.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.services.ChecklistResponseSubmissionPresignedService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * FASE 3 do fluxo presigned — submissão do checklist com as fotos já enviadas ao
 * S3 (cada foto referencia sua imagem por {@code objectKey}). Persistência
 * transacional (tudo-ou-nada), mesmas validações do submit base64.
 */
@RestController
@RequestMapping("/checklist-responses/submit-presigned")
@RequiredArgsConstructor
public class ChecklistResponseSubmissionPresignedController {

    private final ChecklistResponseSubmissionPresignedService submissionService;

    @PostMapping
    public ResponseEntity<WebResponseEntity<ChecklistResponseEntity>> submitChecklistResponse(
            @Valid @RequestBody ChecklistResponseSubmissionDTO submissionDto) {

        ChecklistResponseEntity result = submissionService.submitChecklistResponse(submissionDto);

        WebResponseEntity<ChecklistResponseEntity> response = WebResponseEntity.success(
                result,
                "Resposta de checklist e questionários submetida com sucesso!"
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
