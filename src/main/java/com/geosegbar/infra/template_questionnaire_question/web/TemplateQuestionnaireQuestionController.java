package com.geosegbar.infra.template_questionnaire_question.web;

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
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.infra.template_questionnaire_question.dtos.QuestionReorderDTO;
import com.geosegbar.infra.template_questionnaire_question.services.TemplateQuestionnaireQuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/template-questionnaire-questions")
@RequiredArgsConstructor
public class TemplateQuestionnaireQuestionController {

    private final TemplateQuestionnaireQuestionService templateQuestionnaireQuestionService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<TemplateQuestionnaireQuestionEntity>>> getAllTemplateQuestions() {
        List<TemplateQuestionnaireQuestionEntity> list = templateQuestionnaireQuestionService.findAll();
        WebResponseEntity<List<TemplateQuestionnaireQuestionEntity>> response = WebResponseEntity.success(list, "Questões do template obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<TemplateQuestionnaireQuestionEntity>> getTemplateQuestionById(@PathVariable Long id) {
        TemplateQuestionnaireQuestionEntity entity = templateQuestionnaireQuestionService.findById(id);
        WebResponseEntity<TemplateQuestionnaireQuestionEntity> response = WebResponseEntity.success(entity, "Questão do template obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<TemplateQuestionnaireQuestionEntity>> createTemplateQuestion(@Valid @RequestBody TemplateQuestionnaireQuestionEntity entity) {
        TemplateQuestionnaireQuestionEntity created = templateQuestionnaireQuestionService.save(entity);
        WebResponseEntity<TemplateQuestionnaireQuestionEntity> response = WebResponseEntity.success(created, "Questão do template criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/reorder")
    public ResponseEntity<WebResponseEntity<List<TemplateQuestionnaireQuestionEntity>>> reorderQuestions(
        @Valid @RequestBody QuestionReorderDTO reorderDTO) {
    
    List<TemplateQuestionnaireQuestionEntity> reorderedQuestions = 
            templateQuestionnaireQuestionService.reorderQuestions(reorderDTO);
    
    WebResponseEntity<List<TemplateQuestionnaireQuestionEntity>> response = 
            WebResponseEntity.success(
                reorderedQuestions, 
                "Questões reordenadas com sucesso!"
            );
    
    return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<TemplateQuestionnaireQuestionEntity>> updateTemplateQuestion(@PathVariable Long id, @Valid @RequestBody TemplateQuestionnaireQuestionEntity entity) {
        entity.setId(id);
        TemplateQuestionnaireQuestionEntity updated = templateQuestionnaireQuestionService.update(entity);
        WebResponseEntity<TemplateQuestionnaireQuestionEntity> response = WebResponseEntity.success(updated, "Questão do template atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteTemplateQuestion(@PathVariable Long id) {
        templateQuestionnaireQuestionService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Questão do template excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}