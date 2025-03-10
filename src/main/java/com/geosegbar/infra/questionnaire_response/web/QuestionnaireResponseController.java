package com.geosegbar.infra.questionnaire_response.web;

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
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.infra.questionnaire_response.services.QuestionnaireResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/questionnaire-responses")
@RequiredArgsConstructor
public class QuestionnaireResponseController {

    private final QuestionnaireResponseService questionnaireResponseService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<QuestionnaireResponseEntity>>> getAllResponses() {
        List<QuestionnaireResponseEntity> responses = questionnaireResponseService.findAll();
        WebResponseEntity<List<QuestionnaireResponseEntity>> response = WebResponseEntity.success(responses, "Respostas do questionário obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<QuestionnaireResponseEntity>> getResponseById(@PathVariable Long id) {
        QuestionnaireResponseEntity entity = questionnaireResponseService.findById(id);
        WebResponseEntity<QuestionnaireResponseEntity> response = WebResponseEntity.success(entity, "Resposta do questionário obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<QuestionnaireResponseEntity>> createResponse(@Valid @RequestBody QuestionnaireResponseEntity entity) {
        QuestionnaireResponseEntity created = questionnaireResponseService.save(entity);
        WebResponseEntity<QuestionnaireResponseEntity> response = WebResponseEntity.success(created, "Resposta do questionário criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<QuestionnaireResponseEntity>> updateResponse(@PathVariable Long id, @Valid @RequestBody QuestionnaireResponseEntity entity) {
        entity.setId(id);
        QuestionnaireResponseEntity updated = questionnaireResponseService.update(entity);
        WebResponseEntity<QuestionnaireResponseEntity> response = WebResponseEntity.success(updated, "Resposta do questionário atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteResponse(@PathVariable Long id) {
        questionnaireResponseService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Resposta do questionário excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}