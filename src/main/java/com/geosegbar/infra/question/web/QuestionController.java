package com.geosegbar.infra.question.web;

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
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.infra.question.services.QuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<QuestionEntity>>> getAllQuestions() {
        List<QuestionEntity> questions = questionService.findAll();
        WebResponseEntity<List<QuestionEntity>> response = WebResponseEntity.success(questions, "Questões obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<WebResponseEntity<List<QuestionEntity>>> getQuestionsByClient(@PathVariable Long clientId) {
        List<QuestionEntity> questions = questionService.findByClientIdOrderedByText(clientId);
        WebResponseEntity<List<QuestionEntity>> response = WebResponseEntity.success(
                questions,
                "Questões do cliente obtidas com sucesso!"
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<QuestionEntity>> getQuestionById(@PathVariable Long id) {
        QuestionEntity question = questionService.findById(id);
        WebResponseEntity<QuestionEntity> response = WebResponseEntity.success(question, "Questão obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<QuestionEntity>> createQuestion(@Valid @RequestBody QuestionEntity question) {
        QuestionEntity created = questionService.save(question);
        WebResponseEntity<QuestionEntity> response = WebResponseEntity.success(created, "Questão criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<QuestionEntity>> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionEntity question) {
        question.setId(id);
        QuestionEntity updated = questionService.update(question);
        WebResponseEntity<QuestionEntity> response = WebResponseEntity.success(updated, "Questão atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/confirm-update")
    public ResponseEntity<WebResponseEntity<QuestionEntity>> confirmUpdateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionEntity question) {
        question.setId(id);
        QuestionEntity updated = questionService.confirmUpdate(question);
        WebResponseEntity<QuestionEntity> response = WebResponseEntity.success(
                updated,
                "Questão atualizada com sucesso!"
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteQuestion(@PathVariable Long id) {
        questionService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Questão excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}
