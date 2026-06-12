package com.geosegbar.infra.question.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.template_questionnaire_question.persistence.jpa.TemplateQuestionnaireQuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ClientRepository clientRepository;
    private final AnswerRepository answerRepository;
    private final TemplateQuestionnaireQuestionRepository templateQuestionnaireQuestionRepository;

    @Transactional
    public void deleteById(Long id) {
        log.info("Iniciando exclusão da questão {}", id);

        QuestionEntity question = questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Questão não encontrada para exclusão com ID: " + id));

        List<com.geosegbar.entities.AnswerEntity> answers = answerRepository.findByQuestionIdWithDetails(id);
        if (!answers.isEmpty()) {
            throw new BusinessRuleException(
                    "Não é possível excluir a questão '" + question.getQuestionText()
                    + "' pois existem " + answers.size() + " resposta(s) registrada(s) associadas a ela. "
                    + "A exclusão impactaria na consistência dos dados históricos.");
        }

        log.info("Questão {} não possui respostas registradas. Verificando uso em templates.", id);

        List<com.geosegbar.entities.TemplateQuestionnaireQuestionEntity> templateQuestions
                = templateQuestionnaireQuestionRepository.findByQuestionId(id);

        if (!templateQuestions.isEmpty()) {
            log.info("Questão {} está em {} template(s). Removendo relacionamentos.",
                    id, templateQuestions.size());

            for (com.geosegbar.entities.TemplateQuestionnaireQuestionEntity tq : templateQuestions) {
                templateQuestionnaireQuestionRepository.deleteById(tq.getId());
                log.info("Relacionamento removido: TemplateQuestionnaireQuestion ID {} do Template '{}'",
                        tq.getId(), tq.getTemplateQuestionnaire().getName());
            }
        } else {
            log.info("Questão {} não está sendo usada em nenhum template.", id);
        }

        questionRepository.deleteById(id);
        log.info("Questão {} deletada com sucesso.", id);
    }

    @Transactional
    public QuestionEntity save(QuestionEntity question) {
        if (question.getClient() == null || question.getClient().getId() == null) {
            throw new InvalidInputException("Questão deve estar associada a um cliente!");
        }

        if (!clientRepository.existsById(question.getClient().getId())) {
            throw new NotFoundException("Cliente não encontrado com ID: " + question.getClient().getId());
        }

        validateQuestionByType(question);
        QuestionEntity saved = questionRepository.save(question);

        log.info("Questão {} criada.", saved.getId());
        return saved;
    }

    @Transactional
    public QuestionEntity update(QuestionEntity question) {
        return update(question, true, null);
    }

    /**
     * Atualiza uma questão respeitando a política de propagação.
     *
     * <p>
     * {@code applyToAll=true}: edita a questão in-place (reflete em todos os
     * questionários que a usam). {@code applyToAll=false}: se a questão estiver
     * em outros questionários além do {@code originTemplateId}, cria uma nova
     * questão com as alterações e a substitui apenas nesse template (mesma
     * posição); a questão original é preservada. Se não estiver em nenhum outro,
     * edita in-place mesmo (não há motivo para duplicar).
     */
    @Transactional
    public QuestionEntity update(QuestionEntity question, boolean applyToAll, Long originTemplateId) {
        QuestionEntity existingQuestion = questionRepository.findById(question.getId())
                .orElseThrow(() -> new NotFoundException("Questão não encontrada para atualização!"));

        validateClient(question);
        validateQuestionByType(question);

        boolean hasChanges = hasSignificantChanges(existingQuestion, question);

        if (hasChanges) {
            List<com.geosegbar.entities.AnswerEntity> answers = answerRepository.findByQuestionIdWithDetails(question.getId());

            if (!answers.isEmpty()) {
                throw new BusinessRuleException(
                        "Falha ao atualizar a questão '" + existingQuestion.getQuestionText()
                        + "' pois existem " + answers.size() + " resposta(s) registrada(s) associadas a ela. "
                        + "Deseja atualizar mesmo assim? Isso atualizará a questão até mesmo nas respostas históricas.");
            }
        }

        return applyQuestionUpdate(existingQuestion, question, applyToAll, originTemplateId, false);
    }

    @Transactional
    public QuestionEntity confirmUpdate(QuestionEntity question) {
        return confirmUpdate(question, true, null);
    }

    @Transactional
    public QuestionEntity confirmUpdate(QuestionEntity question, boolean applyToAll, Long originTemplateId) {
        log.warn("Atualização CONFIRMADA da questão {} que possui respostas registradas.", question.getId());

        QuestionEntity existingQuestion = questionRepository.findById(question.getId())
                .orElseThrow(() -> new NotFoundException("Questão não encontrada para atualização!"));

        validateClient(question);
        validateQuestionByType(question);

        List<com.geosegbar.entities.AnswerEntity> answers = answerRepository.findByQuestionIdWithDetails(question.getId());
        if (!answers.isEmpty()) {
            log.warn("ATENÇÃO: Atualizando questão '{}' que possui {} resposta(s) registrada(s). "
                    + "Esta operação pode impactar a consistência dos dados históricos.",
                    existingQuestion.getQuestionText(), answers.size());
        }

        return applyQuestionUpdate(existingQuestion, question, applyToAll, originTemplateId, true);
    }

    /**
     * Núcleo compartilhado da atualização de questão com copy-on-write.
     * Reaproveitado por {@link #update} e {@link #confirmUpdate}.
     */
    private QuestionEntity applyQuestionUpdate(
            QuestionEntity existingQuestion,
            QuestionEntity incoming,
            boolean applyToAll,
            Long originTemplateId,
            boolean confirmed) {

        // Propagar para todos: edição in-place na própria questão.
        if (applyToAll) {
            return saveInPlace(incoming, confirmed);
        }

        // Não propagar: só faz sentido copiar se a questão estiver em outros
        // questionários além do template de origem. Caso contrário, edita in-place.
        long otherUsages = (originTemplateId != null)
                ? templateQuestionnaireQuestionRepository.countByQuestionIdAndTemplateIdNot(existingQuestion.getId(), originTemplateId)
                : templateQuestionnaireQuestionRepository.countByQuestionId(existingQuestion.getId());

        if (otherUsages == 0) {
            log.info("Questão {} não está em outros questionários; aplicando edição in-place mesmo com applyToAll=false.",
                    existingQuestion.getId());
            return saveInPlace(incoming, confirmed);
        }

        // Questão compartilhada: precisa do template de origem para saber onde substituir.
        if (originTemplateId == null) {
            throw new InvalidInputException(
                    "A questão está em outros questionários. Para alterá-la sem afetar os demais, "
                    + "é necessário informar o templateId de origem da edição.");
        }

        return copyOnWrite(existingQuestion, incoming, originTemplateId, confirmed);
    }

    /**
     * Cria uma nova questão com as alterações (reusando as mesmas OptionEntity)
     * e a substitui no template de origem, na mesma posição. A questão original
     * é preservada e continua disponível para revínculo.
     */
    private QuestionEntity copyOnWrite(
            QuestionEntity existingQuestion,
            QuestionEntity incoming,
            Long originTemplateId,
            boolean confirmed) {

        com.geosegbar.entities.TemplateQuestionnaireQuestionEntity association
                = templateQuestionnaireQuestionRepository
                        .findByTemplateQuestionnaireIdAndQuestionId(originTemplateId, existingQuestion.getId())
                        .orElseThrow(() -> new InvalidInputException(
                        "A questão informada não está associada ao template de origem (templateId="
                        + originTemplateId + ")."));

        QuestionEntity copy = new QuestionEntity();
        copy.setQuestionText(incoming.getQuestionText());
        copy.setType(incoming.getType());
        copy.setClient(existingQuestion.getClient());
        // Reusa as mesmas OptionEntity (catálogo compartilhado).
        copy.setOptions(incoming.getOptions() != null
                ? new HashSet<>(incoming.getOptions()) : new HashSet<>());

        QuestionEntity savedCopy = questionRepository.save(copy);

        // Substitui a questão apenas nesta associação (mesmo orderIndex), mantendo
        // a questão original intacta nas demais associações.
        association.setQuestion(savedCopy);
        templateQuestionnaireQuestionRepository.save(association);

        log.info("Copy-on-write: questão {} substituída pela nova questão {} no template {} (orderIndex {}). "
                + "Questão original preservada.{}",
                existingQuestion.getId(), savedCopy.getId(), originTemplateId, association.getOrderIndex(),
                confirmed ? " [CONFIRMADO]" : "");

        return savedCopy;
    }

    private QuestionEntity saveInPlace(QuestionEntity question, boolean confirmed) {
        QuestionEntity saved = questionRepository.save(question);
        log.info("Questão {} atualizada in-place.{}", question.getId(), confirmed ? " [CONFIRMADO]" : "");
        return saved;
    }

    private void validateClient(QuestionEntity question) {
        if (question.getClient() == null || question.getClient().getId() == null) {
            throw new InvalidInputException("Questão deve estar associada a um cliente!");
        }
        if (!clientRepository.existsById(question.getClient().getId())) {
            throw new NotFoundException("Cliente não encontrado com ID: " + question.getClient().getId());
        }
    }

    private boolean hasSignificantChanges(QuestionEntity existing, QuestionEntity updated) {
        if (!existing.getQuestionText().equals(updated.getQuestionText())) {
            log.info("Mudança detectada no texto da questão {}: '{}' -> '{}'",
                    existing.getId(), existing.getQuestionText(), updated.getQuestionText());
            return true;
        }

        if (!existing.getType().equals(updated.getType())) {
            log.info("Mudança detectada no tipo da questão {}: {} -> {}",
                    existing.getId(), existing.getType(), updated.getType());
            return true;
        }

        if (TypeQuestionEnum.CHECKBOX.equals(existing.getType())) {
            Set<Long> existingOptionIds = existing.getOptions().stream()
                    .map(com.geosegbar.entities.OptionEntity::getId)
                    .collect(Collectors.toSet());

            Set<Long> updatedOptionIds = updated.getOptions().stream()
                    .map(com.geosegbar.entities.OptionEntity::getId)
                    .collect(Collectors.toSet());

            if (!existingOptionIds.equals(updatedOptionIds)) {
                log.info("Mudança detectada nas opções da questão {}: {} -> {}",
                        existing.getId(), existingOptionIds, updatedOptionIds);
                return true;
            }
        }

        log.debug("Nenhuma mudança significativa detectada na questão {}", existing.getId());
        return false;
    }

    @Transactional(readOnly = true)
    public QuestionEntity findById(Long id) {

        return questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Questão não encontrada!"));
    }

    @Transactional(readOnly = true)
    public List<QuestionEntity> findAll() {
        return questionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<QuestionEntity> findByClientIdOrderedByText(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new NotFoundException("Cliente não encontrado com ID: " + clientId);
        }
        return questionRepository.findByClientIdOrderByQuestionTextAsc(clientId);
    }

    private void validateQuestionByType(QuestionEntity question) {
        if (TypeQuestionEnum.CHECKBOX.equals(question.getType())) {
            if (question.getOptions() == null || question.getOptions().isEmpty()) {
                throw new InvalidInputException("Questões do tipo CHECKBOX devem ter pelo menos uma opção associada!");
            }
        } else if (TypeQuestionEnum.TEXT.equals(question.getType())) {
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                throw new InvalidInputException("Questões do tipo TEXT não devem ter opções associadas!");
            }
        }
    }
}
