package com.geosegbar.infra.answer.web;

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
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.infra.answer.services.AnswerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<AnswerEntity>>> getAllAnswers() {
        List<AnswerEntity> answers = answerService.findAll();
        WebResponseEntity<List<AnswerEntity>> response = WebResponseEntity.success(answers, "Respostas obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<AnswerEntity>> getAnswerById(@PathVariable Long id) {
        AnswerEntity answer = answerService.findById(id);
        WebResponseEntity<AnswerEntity> response = WebResponseEntity.success(answer, "Resposta obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<AnswerEntity>> createAnswer(@Valid @RequestBody AnswerEntity answer) {
        AnswerEntity created = answerService.save(answer);
        WebResponseEntity<AnswerEntity> response = WebResponseEntity.success(created, "Resposta criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<AnswerEntity>> updateAnswer(@PathVariable Long id, @Valid @RequestBody AnswerEntity answer) {
        answer.setId(id);
        AnswerEntity updated = answerService.update(answer);
        WebResponseEntity<AnswerEntity> response = WebResponseEntity.success(updated, "Resposta atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteAnswer(@PathVariable Long id) {
        answerService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Resposta excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}
