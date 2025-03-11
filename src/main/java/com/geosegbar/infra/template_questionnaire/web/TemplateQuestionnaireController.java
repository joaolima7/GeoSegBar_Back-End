package com.geosegbar.infra.template_questionnaire.web;

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
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.infra.template_questionnaire.services.TemplateQuestionnaireService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/template-questionnaires")
@RequiredArgsConstructor
public class TemplateQuestionnaireController {

    private final TemplateQuestionnaireService templateQuestionnaireService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<TemplateQuestionnaireEntity>>> getAllTemplates() {
        List<TemplateQuestionnaireEntity> templates = templateQuestionnaireService.findAll();
        WebResponseEntity<List<TemplateQuestionnaireEntity>> response = WebResponseEntity.success(templates, "Templates obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<TemplateQuestionnaireEntity>> getTemplateById(@PathVariable Long id) {
        TemplateQuestionnaireEntity template = templateQuestionnaireService.findById(id);
        WebResponseEntity<TemplateQuestionnaireEntity> response = WebResponseEntity.success(template, "Template obtido com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/checklist/{checklistId}")
    public ResponseEntity<WebResponseEntity<List<TemplateQuestionnaireEntity>>> getTemplatesByChecklistId(@PathVariable Long checklistId) {
        List<TemplateQuestionnaireEntity> templates = templateQuestionnaireService.findByChecklistId(checklistId);
        WebResponseEntity<List<TemplateQuestionnaireEntity>> response = WebResponseEntity.success(templates, "Templates do checklist obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<TemplateQuestionnaireEntity>> createTemplate(@Valid @RequestBody TemplateQuestionnaireEntity template) {
        TemplateQuestionnaireEntity created = templateQuestionnaireService.save(template);
        WebResponseEntity<TemplateQuestionnaireEntity> response = WebResponseEntity.success(created, "Template criado com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<TemplateQuestionnaireEntity>> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateQuestionnaireEntity template) {
        template.setId(id);
        TemplateQuestionnaireEntity updated = templateQuestionnaireService.update(template);
        WebResponseEntity<TemplateQuestionnaireEntity> response = WebResponseEntity.success(updated, "Template atualizado com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteTemplate(@PathVariable Long id) {
        templateQuestionnaireService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Template excluído com sucesso!");
        return ResponseEntity.ok(response);
    }
}