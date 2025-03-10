package com.geosegbar.infra.template_questionnaire_question.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.template_questionnaire_question.persistence.jpa.TemplateQuestionnaireQuestionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateQuestionnaireQuestionService {

    private final TemplateQuestionnaireQuestionRepository tqQuestionRepository;

    @Transactional
    public void deleteById(Long id) {
        tqQuestionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Questão do template não encontrada para exclusão!"));
        tqQuestionRepository.deleteById(id);
    }

    @Transactional
    public TemplateQuestionnaireQuestionEntity save(TemplateQuestionnaireQuestionEntity tqQuestion) {
        return tqQuestionRepository.save(tqQuestion);
    }

    @Transactional
    public TemplateQuestionnaireQuestionEntity update(TemplateQuestionnaireQuestionEntity tqQuestion) {
        tqQuestionRepository.findById(tqQuestion.getId())
            .orElseThrow(() -> new NotFoundException("Questão do template não encontrada para atualização!"));
        return tqQuestionRepository.save(tqQuestion);
    }

    public TemplateQuestionnaireQuestionEntity findById(Long id) {
        return tqQuestionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Questão do template não encontrada!"));
    }

    public List<TemplateQuestionnaireQuestionEntity> findAll() {
        return tqQuestionRepository.findAll();
    }
}
