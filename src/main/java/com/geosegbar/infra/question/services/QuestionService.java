package com.geosegbar.infra.question.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.exceptions.InvalidInputException;
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
        validateQuestionByType(question);
        return questionRepository.save(question);
    }

    @Transactional
    public QuestionEntity update(QuestionEntity question) {
        questionRepository.findById(question.getId())
            .orElseThrow(() -> new NotFoundException("Questão não encontrada para atualização!"));
        validateQuestionByType(question);
        return questionRepository.save(question);
    }

    public QuestionEntity findById(Long id) {
        return questionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Questão não encontrada!"));
    }

    public List<QuestionEntity> findAll() {
        return questionRepository.findAll();
    }

    private void validateQuestionByType(QuestionEntity question) {
        if (TypeQuestionEnum.CHECKBOX.equals(question.getType())) {
            if (question.getOptions() == null || question.getOptions().isEmpty()) {
                throw new InvalidInputException("Questões do tipo CHECKBOX devem ter pelo menos uma opção associada!");
            }
        } else if (TypeQuestionEnum.TEXT.equals(question.getType())) {
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                throw new InvalidInputException("Questões do tipo TEXT não devem ter opções associadas!");
            }
        }
    }
}