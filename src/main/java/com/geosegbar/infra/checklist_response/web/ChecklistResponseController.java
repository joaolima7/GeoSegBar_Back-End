package com.geosegbar.infra.checklist_response.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.infra.checklist_response.services.ChecklistResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/checklist-responses")
@RequiredArgsConstructor
public class ChecklistResponseController {

    private final ChecklistResponseService checklistResponseService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<ChecklistResponseEntity>>> getAllChecklistResponses() {
        List<ChecklistResponseEntity> responses = checklistResponseService.findAll();
        WebResponseEntity<List<ChecklistResponseEntity>> response = WebResponseEntity.success(responses, "Respostas de checklist obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ChecklistResponseEntity>> getChecklistResponseById(@PathVariable Long id) {
        ChecklistResponseEntity checklistResponse = checklistResponseService.findById(id);
        WebResponseEntity<ChecklistResponseEntity> response = WebResponseEntity.success(checklistResponse, "Resposta de checklist obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<ChecklistResponseEntity>>> getChecklistResponsesByDamId(@PathVariable Long damId) {
        List<ChecklistResponseEntity> responses = checklistResponseService.findByDamId(damId);
        WebResponseEntity<List<ChecklistResponseEntity>> response = WebResponseEntity.success(responses, "Respostas de checklist da barragem obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<ChecklistResponseEntity>> createChecklistResponse(@Valid @RequestBody ChecklistResponseEntity checklistResponse) {
        ChecklistResponseEntity created = checklistResponseService.save(checklistResponse);
        WebResponseEntity<ChecklistResponseEntity> response = WebResponseEntity.success(created, "Resposta de checklist criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ChecklistResponseEntity>> updateChecklistResponse(@PathVariable Long id, @Valid @RequestBody ChecklistResponseEntity checklistResponse) {
        checklistResponse.setId(id);
        ChecklistResponseEntity updated = checklistResponseService.update(checklistResponse);
        WebResponseEntity<ChecklistResponseEntity> response = WebResponseEntity.success(updated, "Resposta de checklist atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteChecklistResponse(@PathVariable Long id) {
        checklistResponseService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Resposta de checklist excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}