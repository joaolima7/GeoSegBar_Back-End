package com.geosegbar.infra.checklist.web;

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
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersDTO;
import com.geosegbar.infra.checklist.services.ChecklistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<ChecklistEntity>>> getAllChecklists() {
        List<ChecklistEntity> checklists = checklistService.findAll();
        WebResponseEntity<List<ChecklistEntity>> response = WebResponseEntity.success(checklists, "Checklists obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<ChecklistEntity>>> getChecklistsByDam(@PathVariable Long damId) {
        List<ChecklistEntity> checklists = checklistService.findByDamId(damId);
        WebResponseEntity<List<ChecklistEntity>> response = WebResponseEntity.success(checklists, "Checklists para a Barragem obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ChecklistEntity>> getChecklistById(@PathVariable Long id) {
        ChecklistEntity checklist = checklistService.findById(id);
        WebResponseEntity<ChecklistEntity> response = WebResponseEntity.success(checklist, "Checklist obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}/checklist/{checklistId}")
    public ResponseEntity<WebResponseEntity<ChecklistEntity>> getChecklistForDam(
            @PathVariable Long damId,
            @PathVariable Long checklistId) {
        ChecklistEntity checklist = checklistService.findChecklistForDam(damId, checklistId);
        WebResponseEntity<ChecklistEntity> response = WebResponseEntity.success(
                checklist,
                "Checklist encontrado para a barragem especificada!"
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}/with-last-answers")
    public ResponseEntity<WebResponseEntity<List<ChecklistWithLastAnswersDTO>>> getChecklistsWithLastAnswersForDam(
            @PathVariable Long damId) {
        List<ChecklistWithLastAnswersDTO> checklists = checklistService.findChecklistsWithLastAnswersForDam(damId);
        WebResponseEntity<List<ChecklistWithLastAnswersDTO>> response = WebResponseEntity.success(
                checklists,
                "Checklists com últimas respostas não-NI obtidos com sucesso!"
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<ChecklistEntity>> createChecklist(@Valid @RequestBody ChecklistEntity checklist) {
        ChecklistEntity created = checklistService.save(checklist);
        WebResponseEntity<ChecklistEntity> response = WebResponseEntity.success(created, "Checklist criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ChecklistEntity>> updateChecklist(@PathVariable Long id, @Valid @RequestBody ChecklistEntity checklist) {
        checklist.setId(id);
        ChecklistEntity updated = checklistService.update(checklist);
        WebResponseEntity<ChecklistEntity> response = WebResponseEntity.success(updated, "Checklist atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteChecklist(@PathVariable Long id) {
        checklistService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Checklist excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}
