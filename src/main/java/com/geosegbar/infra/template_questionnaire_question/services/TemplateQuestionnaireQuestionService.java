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
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;
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
    private final QuestionRepository questionRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;

    @Transactional
    public void deleteById(Long id) {
        TemplateQuestionnaireQuestionEntity questionToDelete = tqQuestionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Questão do template não encontrada para exclusão!"));

        Long templateId = questionToDelete.getTemplateQuestionnaire().getId();

        if (questionnaireResponseRepository.existsByTemplateQuestionnaireId(templateId)) {
            throw new InvalidInputException(
                    "Não é possível excluir esta questão pois existem questionários respondidos usando este template. "
                    + "Crie um novo template para aplicar as alterações desejadas."
            );
        }

        int deletedIndex = questionToDelete.getOrderIndex();

        tqQuestionRepository.deleteById(id);

        List<TemplateQuestionnaireQuestionEntity> remainingQuestions
                = tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(templateId);

        for (TemplateQuestionnaireQuestionEntity question : remainingQuestions) {
            if (question.getOrderIndex() > deletedIndex) {
                question.setOrderIndex(question.getOrderIndex() - 1);
                tqQuestionRepository.save(question);
            }
        }
    }

    @Transactional
    public TemplateQuestionnaireQuestionEntity save(TemplateQuestionnaireQuestionEntity tqQuestion) {
        questionRepository.findById(tqQuestion.getQuestion().getId())
                .orElseThrow(() -> new NotFoundException("Questão não encontrada!"));

        var template = templateQuestionnaireRepository.findById(tqQuestion.getTemplateQuestionnaire().getId())
                .orElseThrow(() -> new NotFoundException("Template não encontrado!"));
        tqQuestion.setTemplateQuestionnaire(template);

        int currentQuestionCount = tqQuestionRepository.countQuestionsByTemplateId(template.getId());

        Integer requestedIndex = tqQuestion.getOrderIndex();
        if (requestedIndex == null) {
            tqQuestion.setOrderIndex(currentQuestionCount + 1);
        } else if (requestedIndex <= 0) {
            throw new InvalidInputException("O índice de ordem deve ser um número positivo!");
        } else if (requestedIndex > currentQuestionCount + 1) {
            tqQuestion.setOrderIndex(currentQuestionCount + 1);
        } else {
            List<TemplateQuestionnaireQuestionEntity> existingQuestions
                    = tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(template.getId());

            for (TemplateQuestionnaireQuestionEntity existing : existingQuestions) {
                if (existing.getOrderIndex() >= requestedIndex) {
                    existing.setOrderIndex(existing.getOrderIndex() + 1);
                    tqQuestionRepository.save(existing);
                }
            }
        }

        TemplateQuestionnaireQuestionEntity saved = tqQuestionRepository.save(tqQuestion);

        return saved;
    }

    @Transactional
    public TemplateQuestionnaireQuestionEntity update(TemplateQuestionnaireQuestionEntity tqQuestion) {
        var existingQuestion = tqQuestionRepository.findById(tqQuestion.getId())
                .orElseThrow(() -> new NotFoundException("Questão do template não encontrada!"));

        Long templateId = existingQuestion.getTemplateQuestionnaire().getId();
        if (questionnaireResponseRepository.existsByTemplateQuestionnaireId(templateId)) {
            throw new InvalidInputException(
                    "Não é possível modificar esta questão pois existem questionários respondidos usando este template. "
                    + "Crie um novo template para aplicar as alterações desejadas."
            );
        }

        if (tqQuestion.getOrderIndex() != null
                && !tqQuestion.getOrderIndex().equals(existingQuestion.getOrderIndex())) {

            int currentCount = tqQuestionRepository.countQuestionsByTemplateId(templateId);

            if (tqQuestion.getOrderIndex() <= 0) {
                throw new InvalidInputException("O índice de ordem deve ser um número positivo!");
            }

            if (tqQuestion.getOrderIndex() > currentCount) {
                throw new InvalidInputException("O índice de ordem não pode ser maior que o total de questões: " + currentCount);
            }

            List<TemplateQuestionnaireQuestionEntity> questions
                    = tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(templateId);

            int oldIndex = existingQuestion.getOrderIndex();
            int newIndex = tqQuestion.getOrderIndex();

            for (TemplateQuestionnaireQuestionEntity q : questions) {
                if (q.getId().equals(tqQuestion.getId())) {
                    continue;
                }

                if (oldIndex < newIndex) {
                    if (q.getOrderIndex() > oldIndex && q.getOrderIndex() <= newIndex) {
                        q.setOrderIndex(q.getOrderIndex() - 1);
                        tqQuestionRepository.save(q);
                    }
                } else {
                    if (q.getOrderIndex() >= newIndex && q.getOrderIndex() < oldIndex) {
                        q.setOrderIndex(q.getOrderIndex() + 1);
                        tqQuestionRepository.save(q);
                    }
                }
            }

            existingQuestion.setOrderIndex(tqQuestion.getOrderIndex());
            TemplateQuestionnaireQuestionEntity saved = tqQuestionRepository.save(existingQuestion);

            return saved;
        } else {
            throw new InvalidInputException(
                    "Apenas o índice de ordem pode ser atualizado. Nenhuma alteração foi detectada ou solicitada."
            );
        }
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

        List<TemplateQuestionnaireQuestionEntity> existingQuestions
                = tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(templateId);

        if (existingQuestions.isEmpty()) {
            throw new NotFoundException("O template de questionário não contém questões para reordenar");
        }

        if (existingQuestions.size() != reorderDTO.getQuestions().size()) {
            throw new InvalidInputException(
                    "O número de questões informadas (" + reorderDTO.getQuestions().size()
                    + ") não corresponde ao número de questões do template (" + existingQuestions.size() + ")!");
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

        List<TemplateQuestionnaireQuestionEntity> saved = tqQuestionRepository.saveAll(existingQuestions);

        return saved;
    }

    public List<TemplateQuestionnaireQuestionEntity> findAllByTemplateIdOrdered(Long templateId) {
        return tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(templateId);
    }
}
