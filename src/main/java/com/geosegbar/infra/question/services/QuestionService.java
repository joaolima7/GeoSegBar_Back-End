package com.geosegbar.infra.question.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Transactional
    public void deleteById(Long id) {
        questionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Questão não encontrada para exclusão!"));
        questionRepository.deleteById(id);
    }

    @Transactional
    public QuestionEntity save(QuestionEntity question) {
        // Se desejar, implementar verificação de duplicidade aqui.
        return questionRepository.save(question);
    }

    @Transactional
    public QuestionEntity update(QuestionEntity question) {
        questionRepository.findById(question.getId())
            .orElseThrow(() -> new NotFoundException("Questão não encontrada para atualização!"));
        return questionRepository.save(question);
    }

    public QuestionEntity findById(Long id) {
        return questionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Questão não encontrada!"));
    }

    public List<QuestionEntity> findAll() {
        return questionRepository.findAll();
    }
}