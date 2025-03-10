package com.geosegbar.infra.questionnaire_response.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionnaireResponseService {

    private final QuestionnaireResponseRepository responseRepository;

    @Transactional
    public void deleteById(Long id) {
        responseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resposta do questionário não encontrada para exclusão!"));
        responseRepository.deleteById(id);
    }

    @Transactional
    public QuestionnaireResponseEntity save(QuestionnaireResponseEntity response) {
        return responseRepository.save(response);
    }

    @Transactional
    public QuestionnaireResponseEntity update(QuestionnaireResponseEntity response) {
        responseRepository.findById(response.getId())
            .orElseThrow(() -> new NotFoundException("Resposta do questionário não encontrada para atualização!"));
        return responseRepository.save(response);
    }

    public QuestionnaireResponseEntity findById(Long id) {
        return responseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resposta do questionário não encontrada!"));
    }

    public List<QuestionnaireResponseEntity> findAll() {
        return responseRepository.findAll();
    }
}
