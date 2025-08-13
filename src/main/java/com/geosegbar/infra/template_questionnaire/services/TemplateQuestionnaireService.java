package com.geosegbar.infra.template_questionnaire.services;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist.services.ChecklistService;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionDTO;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionnaireCreationDTO;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateQuestionnaireService {

    private final TemplateQuestionnaireRepository templateQuestionnaireRepository;
    private final ChecklistService checklistService;
    private final QuestionRepository questionRepository;

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

    @Transactional
    public TemplateQuestionnaireEntity createWithQuestions(TemplateQuestionnaireCreationDTO dto) {
        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName(dto.getName());
        template.setTemplateQuestions(new HashSet<>());

        template = templateQuestionnaireRepository.save(template);

        for (TemplateQuestionDTO questionDto : dto.getQuestions()) {
            QuestionEntity question = questionRepository.findById(questionDto.getQuestionId())
                    .orElseThrow(() -> new NotFoundException(
                    "Questão não encontrada com ID: " + questionDto.getQuestionId()));

            TemplateQuestionnaireQuestionEntity templateQuestion = new TemplateQuestionnaireQuestionEntity();
            templateQuestion.setTemplateQuestionnaire(template);
            templateQuestion.setQuestion(question);
            templateQuestion.setOrderIndex(questionDto.getOrderIndex());

            template.getTemplateQuestions().add(templateQuestion);
        }

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
        return templates;
    }
}
