package com.geosegbar.infra.template_questionnaire_question.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionAssociationItemDTO;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionAssociationsRequestDTO;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionAssociationsResponseDTO;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;
import com.geosegbar.infra.template_questionnaire_question.dtos.QuestionOrderDTO;
import com.geosegbar.infra.template_questionnaire_question.dtos.QuestionReorderDTO;
import com.geosegbar.infra.template_questionnaire_question.persistence.jpa.TemplateQuestionnaireQuestionRepository;

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

        if (!questionRepository.existsById(tqQuestion.getQuestion().getId())) {
            throw new NotFoundException("Questão não encontrada!");
        }

        TemplateQuestionnaireEntity template = templateQuestionnaireRepository.findById(tqQuestion.getTemplateQuestionnaire().getId())
                .orElseThrow(() -> new NotFoundException("Template não encontrado!"));

        tqQuestion.setTemplateQuestionnaire(template);

        if (tqQuestion.getId() == null) {
            List<TemplateQuestionnaireQuestionEntity> existing
                    = tqQuestionRepository.findAllByTemplateQuestionnaireIdAndQuestionId(
                            template.getId(), tqQuestion.getQuestion().getId());
            if (!existing.isEmpty()) {
                throw new InvalidInputException("A mesma questão não pode ser adicionada mais de uma vez no mesmo template.");
            }
        }

        int currentQuestionCount = tqQuestionRepository.countQuestionsByTemplateId(template.getId());

        Integer requestedIndex = tqQuestion.getOrderIndex();

        if (requestedIndex == null || requestedIndex > currentQuestionCount + 1) {
            tqQuestion.setOrderIndex(currentQuestionCount + 1);
        } else if (requestedIndex <= 0) {
            throw new InvalidInputException("O índice de ordem deve ser um número positivo!");
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

        return tqQuestionRepository.save(tqQuestion);
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
            return tqQuestionRepository.save(existingQuestion);
        } else {
            throw new InvalidInputException(
                    "Apenas o índice de ordem pode ser atualizado. Nenhuma alteração foi detectada ou solicitada."
            );
        }
    }

    @Transactional(readOnly = true)
    public TemplateQuestionnaireQuestionEntity findById(Long id) {
        return tqQuestionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Questão do template não encontrada!"));
    }

    @Transactional(readOnly = true)
    public List<TemplateQuestionnaireQuestionEntity> findAll() {
        return tqQuestionRepository.findAll();
    }

    @Transactional
    public TemplateQuestionAssociationsResponseDTO updateQuestionAssociations(
            Long templateId,
            TemplateQuestionAssociationsRequestDTO dto) {

        if (dto == null) {
            throw new InvalidInputException("Dados de associação de questões são obrigatórios.");
        }

        Set<Long> associateIds = normalizeIds(dto.getAssociateQuestionIds(), "associateQuestionIds");
        Set<Long> disassociateIds = normalizeIds(dto.getDisassociateQuestionIds(), "disassociateQuestionIds");

        if (associateIds.isEmpty() && disassociateIds.isEmpty()) {
            throw new InvalidInputException("Informe ao menos uma questão para associar ou desassociar.");
        }

        return applyQuestionAssociations(templateId, associateIds, disassociateIds, dto.getOrder());
    }

    @Transactional
    public TemplateQuestionAssociationsResponseDTO reorderQuestions(Long templateId, QuestionReorderDTO reorderDTO) {
        if (reorderDTO == null) {
            throw new InvalidInputException("Dados de reordenação de questões são obrigatórios.");
        }

        return applyQuestionAssociations(
                templateId,
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                reorderDTO.getQuestions()
        );
    }

    @Transactional(readOnly = true)
    public List<TemplateQuestionnaireQuestionEntity> findAllByTemplateIdOrdered(Long templateId) {
        return tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(templateId);
    }

    private TemplateQuestionAssociationsResponseDTO applyQuestionAssociations(
            Long templateId,
            Set<Long> associateIds,
            Set<Long> disassociateIds,
            List<QuestionOrderDTO> order) {

        validateNoOverlap(associateIds, disassociateIds);

        TemplateQuestionnaireEntity template = templateQuestionnaireRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template não encontrado com ID: " + templateId));

        List<TemplateQuestionnaireQuestionEntity> existingAssociations
                = tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(templateId);

        Map<Long, TemplateQuestionnaireQuestionEntity> existingByQuestionId = existingAssociations.stream()
                .collect(Collectors.toMap(this::questionIdOf, tqq -> tqq));
        Set<Long> existingQuestionIds = new LinkedHashSet<>(existingByQuestionId.keySet());

        validateQuestionAssociationChanges(existingQuestionIds, associateIds, disassociateIds);

        Map<Long, QuestionEntity> questionsToAssociate = findQuestionsById(associateIds);
        validateQuestionsBelongToTemplateClient(template, questionsToAssociate.values());

        Set<Long> finalQuestionIds = new LinkedHashSet<>(existingQuestionIds);
        finalQuestionIds.removeAll(disassociateIds);
        finalQuestionIds.addAll(associateIds);

        Map<Long, Integer> orderByQuestionId = validateQuestionOrder(order, finalQuestionIds);

        for (Long questionId : disassociateIds) {
            tqQuestionRepository.deleteByIdNative(existingByQuestionId.get(questionId).getId());
        }
        tqQuestionRepository.flush();

        List<TemplateQuestionnaireQuestionEntity> finalAssociations = existingAssociations.stream()
                .filter(tqq -> !disassociateIds.contains(questionIdOf(tqq)))
                .collect(Collectors.toCollection(ArrayList::new));

        for (Long questionId : associateIds) {
            TemplateQuestionnaireQuestionEntity newAssociation = new TemplateQuestionnaireQuestionEntity();
            newAssociation.setTemplateQuestionnaire(template);
            newAssociation.setQuestion(questionsToAssociate.get(questionId));
            newAssociation.setOrderIndex(orderByQuestionId.get(questionId));
            finalAssociations.add(tqQuestionRepository.save(newAssociation));
        }

        for (TemplateQuestionnaireQuestionEntity association : finalAssociations) {
            association.setOrderIndex(orderByQuestionId.get(questionIdOf(association)));
        }

        List<TemplateQuestionnaireQuestionEntity> savedAssociations = tqQuestionRepository.saveAll(finalAssociations);
        savedAssociations.sort(Comparator.comparing(TemplateQuestionnaireQuestionEntity::getOrderIndex));

        return buildQuestionAssociationsResponse(templateId, associateIds, disassociateIds, savedAssociations);
    }

    private Set<Long> normalizeIds(List<Long> ids, String fieldName) {
        Set<Long> normalized = new LinkedHashSet<>();
        if (ids == null) {
            return normalized;
        }

        for (Long id : ids) {
            if (id == null) {
                throw new InvalidInputException("O campo " + fieldName + " não aceita IDs nulos.");
            }
            if (!normalized.add(id)) {
                throw new InvalidInputException("ID duplicado em " + fieldName + ": " + id);
            }
        }

        return normalized;
    }

    private void validateNoOverlap(Set<Long> associateIds, Set<Long> disassociateIds) {
        Set<Long> overlap = new LinkedHashSet<>(associateIds);
        overlap.retainAll(disassociateIds);
        if (!overlap.isEmpty()) {
            throw new InvalidInputException(
                    "A mesma questão não pode ser associada e desassociada na mesma requisição: " + overlap);
        }
    }

    private void validateQuestionAssociationChanges(
            Set<Long> existingQuestionIds,
            Set<Long> associateIds,
            Set<Long> disassociateIds) {

        Set<Long> alreadyAssociated = new LinkedHashSet<>(associateIds);
        alreadyAssociated.retainAll(existingQuestionIds);
        if (!alreadyAssociated.isEmpty()) {
            throw new InvalidInputException("Questões já associadas a este template: " + alreadyAssociated);
        }

        Set<Long> notAssociated = new LinkedHashSet<>(disassociateIds);
        notAssociated.removeAll(existingQuestionIds);
        if (!notAssociated.isEmpty()) {
            throw new InvalidInputException("Questões não associadas a este template: " + notAssociated);
        }
    }

    private Map<Long, QuestionEntity> findQuestionsById(Set<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        List<QuestionEntity> questions = questionRepository.findAllById(questionIds);
        Set<Long> foundIds = questions.stream()
                .map(QuestionEntity::getId)
                .collect(Collectors.toSet());

        Set<Long> missingIds = new LinkedHashSet<>(questionIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Questões não encontradas: " + missingIds);
        }

        return questions.stream()
                .collect(Collectors.toMap(QuestionEntity::getId, question -> question));
    }

    private void validateQuestionsBelongToTemplateClient(
            TemplateQuestionnaireEntity template,
            Iterable<QuestionEntity> questions) {

        if (template.getDam() == null || template.getDam().getClient() == null) {
            throw new InvalidInputException("Template não possui barragem/cliente associado para validar as questões.");
        }

        Long templateClientId = template.getDam().getClient().getId();
        List<Long> invalidQuestionIds = new ArrayList<>();

        for (QuestionEntity question : questions) {
            if (question.getClient() == null || !templateClientId.equals(question.getClient().getId())) {
                invalidQuestionIds.add(question.getId());
            }
        }

        if (!invalidQuestionIds.isEmpty()) {
            throw new InvalidInputException(
                    "Não é possível associar questões de outro cliente ao template: " + invalidQuestionIds);
        }
    }

    private Map<Long, Integer> validateQuestionOrder(List<QuestionOrderDTO> order, Set<Long> finalQuestionIds) {
        if (order == null) {
            throw new InvalidInputException("Lista de ordenação final de questões é obrigatória.");
        }

        if (order.size() != finalQuestionIds.size()) {
            throw new InvalidInputException(
                    "A ordenação final deve conter exatamente as questões finais do template. "
                    + "Informadas: " + order.size() + ", esperadas: " + finalQuestionIds.size() + ".");
        }

        Set<Long> orderedQuestionIds = new LinkedHashSet<>();
        Set<Integer> orderIndexes = new HashSet<>();
        Map<Long, Integer> orderByQuestionId = order.stream()
                .peek(orderDto -> {
                    if (orderDto == null) {
                        throw new InvalidInputException("A lista de ordenação não aceita itens nulos.");
                    }
                    if (orderDto.getQuestionId() == null) {
                        throw new InvalidInputException("ID da questão é obrigatório na ordenação.");
                    }
                    if (orderDto.getOrderIndex() == null) {
                        throw new InvalidInputException("Índice de ordem é obrigatório na ordenação.");
                    }
                    if (!orderedQuestionIds.add(orderDto.getQuestionId())) {
                        throw new InvalidInputException("Questão duplicada na ordenação: " + orderDto.getQuestionId());
                    }
                    if (!orderIndexes.add(orderDto.getOrderIndex())) {
                        throw new InvalidInputException("Índice de ordem duplicado: " + orderDto.getOrderIndex());
                    }
                })
                .collect(Collectors.toMap(QuestionOrderDTO::getQuestionId, QuestionOrderDTO::getOrderIndex));

        Set<Long> missingQuestionIds = new LinkedHashSet<>(finalQuestionIds);
        missingQuestionIds.removeAll(orderedQuestionIds);
        Set<Long> invalidQuestionIds = new LinkedHashSet<>(orderedQuestionIds);
        invalidQuestionIds.removeAll(finalQuestionIds);

        if (!missingQuestionIds.isEmpty() || !invalidQuestionIds.isEmpty()) {
            throw new InvalidInputException(
                    "A ordenação final não corresponde às questões finais. "
                    + "Faltando: " + missingQuestionIds + ". Inválidas: " + invalidQuestionIds + ".");
        }

        for (int i = 1; i <= finalQuestionIds.size(); i++) {
            if (!orderIndexes.contains(i)) {
                throw new InvalidInputException(
                        "Índices de ordem devem ser uma sequência contínua de 1 a " + finalQuestionIds.size() + ".");
            }
        }

        return orderByQuestionId;
    }

    private TemplateQuestionAssociationsResponseDTO buildQuestionAssociationsResponse(
            Long templateId,
            Set<Long> associateIds,
            Set<Long> disassociateIds,
            List<TemplateQuestionnaireQuestionEntity> associations) {

        List<TemplateQuestionAssociationItemDTO> items = associations.stream()
                .sorted(Comparator.comparing(TemplateQuestionnaireQuestionEntity::getOrderIndex))
                .map(tqq -> new TemplateQuestionAssociationItemDTO(
                        tqq.getId(),
                        questionIdOf(tqq),
                        tqq.getOrderIndex()
                ))
                .collect(Collectors.toList());

        return new TemplateQuestionAssociationsResponseDTO(
                templateId,
                new ArrayList<>(associateIds),
                new ArrayList<>(disassociateIds),
                items.size(),
                items
        );
    }

    private Long questionIdOf(TemplateQuestionnaireQuestionEntity tqq) {
        return tqq.getQuestion().getId();
    }
}
