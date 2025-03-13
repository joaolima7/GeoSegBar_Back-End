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
import com.geosegbar.infra.checklist_submission.services.ChecklistResponseSubmissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/checklist-responses/submit")
@RequiredArgsConstructor
public class ChecklistResponseSubmissionController {

    private final ChecklistResponseSubmissionService checklistResponseSubmissionService;
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<ChecklistResponseEntity>> submitChecklistResponse(
            @Valid @RequestBody ChecklistResponseSubmissionDTO submissionDto) {
        
        ChecklistResponseEntity result = checklistResponseSubmissionService.submitChecklistResponse(submissionDto);
        
        WebResponseEntity<ChecklistResponseEntity> response = WebResponseEntity.success(
            result, 
            "Resposta de checklist e questionários submetida com sucesso!"
        );
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}