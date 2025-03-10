package com.geosegbar.infra.answer.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;

    @Transactional
    public void deleteById(Long id) {
        answerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resposta não encontrada para exclusão!"));
        answerRepository.deleteById(id);
    }

    @Transactional
    public AnswerEntity save(AnswerEntity answer) {
        return answerRepository.save(answer);
    }

    @Transactional
    public AnswerEntity update(AnswerEntity answer) {
        answerRepository.findById(answer.getId())
            .orElseThrow(() -> new NotFoundException("Resposta não encontrada para atualização!"));
        return answerRepository.save(answer);
    }

    public AnswerEntity findById(Long id) {
        return answerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resposta não encontrada!"));
    }

    public List<AnswerEntity> findAll() {
        return answerRepository.findAll();
    }
}
