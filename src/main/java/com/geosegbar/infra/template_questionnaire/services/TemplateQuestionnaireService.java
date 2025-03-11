package com.geosegbar.infra.template_questionnaire.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist.services.ChecklistService;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateQuestionnaireService {

    private final TemplateQuestionnaireRepository templateQuestionnaireRepository;
    private final ChecklistService checklistService;

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

    public List<TemplateQuestionnaireEntity> findByChecklistId(Long checklistId) {
        checklistService.findById(checklistId);
        
        List<TemplateQuestionnaireEntity> templates = templateQuestionnaireRepository.findByChecklistsId(checklistId);
        if (templates.isEmpty()) {
            throw new NotFoundException("Nenhum modelo de questionário encontrado para o Checklist com id: " + checklistId);
        }
        return templates;
    }
}
