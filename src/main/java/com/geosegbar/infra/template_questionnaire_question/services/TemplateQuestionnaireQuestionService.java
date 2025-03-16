package com.geosegbar.infra.template_questionnaire_question.services;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;
import com.geosegbar.infra.template_questionnaire_question.dtos.QuestionOrderDTO;
import com.geosegbar.infra.template_questionnaire_question.dtos.QuestionReorderDTO;
import com.geosegbar.infra.template_questionnaire_question.persistence.jpa.TemplateQuestionnaireQuestionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateQuestionnaireQuestionService {

    private final TemplateQuestionnaireQuestionRepository tqQuestionRepository;
    private final TemplateQuestionnaireRepository templateQuestionnaireRepository;

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

    @Transactional
    public List<TemplateQuestionnaireQuestionEntity> reorderQuestions(QuestionReorderDTO reorderDTO) {
    Long templateId = reorderDTO.getTemplateQuestionnaireId();
    if (!templateQuestionnaireRepository.existsById(templateId)) {
        throw new NotFoundException("Template de questionário não encontrado com ID: " + templateId);
    }
    
    List<TemplateQuestionnaireQuestionEntity> existingQuestions = 
        tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(templateId);
    
    if (existingQuestions.isEmpty()) {
        throw new NotFoundException("O template de questionário não contém questões para reordenar");
    }
    
    if (existingQuestions.size() != reorderDTO.getQuestions().size()) {
        throw new InvalidInputException(
            "O número de questões informadas (" + reorderDTO.getQuestions().size() + 
            ") não corresponde ao número de questões do template (" + existingQuestions.size() + ")!");
    }
    
    Set<Long> existingQuestionIds = existingQuestions.stream()
        .map(TemplateQuestionnaireQuestionEntity::getId)
        .collect(Collectors.toSet());
    
    Set<Long> requestQuestionIds = reorderDTO.getQuestions().stream()
        .map(QuestionOrderDTO::getTemplateQuestionId)
        .collect(Collectors.toSet());
    
    if (!existingQuestionIds.containsAll(requestQuestionIds)) {
        Set<Long> invalidIds = new HashSet<>(requestQuestionIds);
        invalidIds.removeAll(existingQuestionIds);
        throw new InvalidInputException(
            "As seguintes questões não pertencem a este template: " + invalidIds);
    }
    
    if (!requestQuestionIds.containsAll(existingQuestionIds)) {
        Set<Long> missingIds = new HashSet<>(existingQuestionIds);
        missingIds.removeAll(requestQuestionIds);
        throw new InvalidInputException(
            "As seguintes questões do template não foram incluídas: " + missingIds);
    }
    
    Set<Integer> requestIndices = reorderDTO.getQuestions().stream()
        .map(QuestionOrderDTO::getOrderIndex)
        .collect(Collectors.toSet());
    
    if (requestIndices.size() != reorderDTO.getQuestions().size()) {
        throw new InvalidInputException("Existem índices duplicados na requisição");
    }
    
    int maxIndex = requestIndices.stream().max(Integer::compare).orElse(0);
    int expectedSize = maxIndex;
    
    if (requestIndices.size() != expectedSize) {
        throw new InvalidInputException(
            "A sequência de índices não é contínua. Deve começar em 1 e não ter lacunas.");
    }
    
    Map<Long, Integer> reorderMap = reorderDTO.getQuestions().stream()
        .collect(Collectors.toMap(
            QuestionOrderDTO::getTemplateQuestionId,
            QuestionOrderDTO::getOrderIndex
        ));
    
    for (TemplateQuestionnaireQuestionEntity question : existingQuestions) {
        question.setOrderIndex(reorderMap.get(question.getId()));
    }
    
    return tqQuestionRepository.saveAll(existingQuestions);
    }

    public List<TemplateQuestionnaireQuestionEntity> findAllByTemplateIdOrdered(Long templateId) {
        return tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(templateId);
    }
}
