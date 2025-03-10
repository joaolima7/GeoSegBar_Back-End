package com.geosegbar.infra.template_questionnaire.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateQuestionnaireService {

    private final TemplateQuestionnaireRepository templateQuestionnaireRepository;

    @Transactional
    public void deleteById(Long id) {
        templateQuestionnaireRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Template não encontrado para exclusão!"));
        templateQuestionnaireRepository.deleteById(id);
    }

    @Transactional
    public TemplateQuestionnaireEntity save(TemplateQuestionnaireEntity template) {
        return templateQuestionnaireRepository.save(template);
    }

    @Transactional
    public TemplateQuestionnaireEntity update(TemplateQuestionnaireEntity template) {
        templateQuestionnaireRepository.findById(template.getId())
            .orElseThrow(() -> new NotFoundException("Template não encontrado para atualização!"));
        return templateQuestionnaireRepository.save(template);
    }

    public TemplateQuestionnaireEntity findById(Long id) {
        return templateQuestionnaireRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Template não encontrado!"));
    }

    public List<TemplateQuestionnaireEntity> findAll() {
        return templateQuestionnaireRepository.findAll();
    }
}
