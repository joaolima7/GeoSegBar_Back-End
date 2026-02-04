package com.geosegbar.infra.questionnaire_response.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireResponseService {

    private final QuestionnaireResponseRepository responseRepository;

    @Transactional
    public void deleteById(Long id) {

        if (!responseRepository.existsById(id)) {
            throw new NotFoundException("Resposta do questionário não encontrada para exclusão!");
        }
        responseRepository.deleteById(id);

        log.info("QuestionnaireResponse {} deletado.", id);
    }

    @Transactional
    public QuestionnaireResponseEntity save(QuestionnaireResponseEntity response) {

        QuestionnaireResponseEntity saved = responseRepository.save(response);

        log.info("QuestionnaireResponse {} criado.", saved.getId());

        return saved;
    }

    @Transactional
    public QuestionnaireResponseEntity update(QuestionnaireResponseEntity response) {
        if (!responseRepository.existsById(response.getId())) {
            throw new NotFoundException("Resposta do questionário não encontrada para atualização!");
        }

        QuestionnaireResponseEntity saved = responseRepository.save(response);

        log.info("QuestionnaireResponse {} atualizado.", response.getId());

        return saved;
    }

    @Transactional(readOnly = true)
    public QuestionnaireResponseEntity findById(Long id) {

        return responseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resposta do questionário não encontrada!"));
    }

    @Transactional(readOnly = true)
    public List<QuestionnaireResponseEntity> findAll() {

        return responseRepository.findAll();
    }
}
