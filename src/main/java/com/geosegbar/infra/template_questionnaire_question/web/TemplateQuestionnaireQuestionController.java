package com.geosegbar.infra.template_questionnaire_question.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.infra.template_questionnaire_question.services.TemplateQuestionnaireQuestionService;

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

}
