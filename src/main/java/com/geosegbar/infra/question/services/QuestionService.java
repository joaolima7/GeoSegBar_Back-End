package com.geosegbar.infra.question.services;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.template_questionnaire_question.persistence.jpa.TemplateQuestionnaireQuestionRepository;

import jakarta.transaction.Transactional;
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
        QuestionEntity existingQuestion = questionRepository.findById(question.getId())
                .orElseThrow(() -> new NotFoundException("Questão não encontrada para atualização!"));

        if (question.getClient() == null || question.getClient().getId() == null) {
            throw new InvalidInputException("Questão deve estar associada a um cliente!");
        }

        if (!clientRepository.existsById(question.getClient().getId())) {
            throw new NotFoundException("Cliente não encontrado com ID: " + question.getClient().getId());
        }

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

        validateQuestionByType(question);
        QuestionEntity saved = questionRepository.save(question);

        log.info("Questão {} atualizada.", question.getId());

        return saved;
    }

    @Transactional
    public QuestionEntity confirmUpdate(QuestionEntity question) {
        log.warn("Atualização CONFIRMADA da questão {} que possui respostas registradas.", question.getId());

        QuestionEntity existingQuestion = questionRepository.findById(question.getId())
                .orElseThrow(() -> new NotFoundException("Questão não encontrada para atualização!"));

        if (question.getClient() == null || question.getClient().getId() == null) {
            throw new InvalidInputException("Questão deve estar associada a um cliente!");
        }

        if (!clientRepository.existsById(question.getClient().getId())) {
            throw new NotFoundException("Cliente não encontrado com ID: " + question.getClient().getId());
        }

        List<com.geosegbar.entities.AnswerEntity> answers = answerRepository.findByQuestionIdWithDetails(question.getId());
        if (!answers.isEmpty()) {
            log.warn("ATENÇÃO: Atualizando questão '{}' que possui {} resposta(s) registrada(s). "
                    + "Esta operação pode impactar a consistência dos dados históricos.",
                    existingQuestion.getQuestionText(), answers.size());

            if (!existingQuestion.getQuestionText().equals(question.getQuestionText())) {
                log.warn("Texto alterado de '{}' para '{}'",
                        existingQuestion.getQuestionText(), question.getQuestionText());
            }

            if (!existingQuestion.getType().equals(question.getType())) {
                log.warn("Tipo alterado de '{}' para '{}'",
                        existingQuestion.getType(), question.getType());
            }
        }

        validateQuestionByType(question);
        QuestionEntity saved = questionRepository.save(question);

        log.info("Questão {} atualizada COM CONFIRMAÇÃO.", question.getId());

        return saved;
    }

    /**
     * Verifica se há mudanças significativas entre a questão existente e a
     * nova. Mudanças significativas são: texto, tipo ou opções.
     *
     * @param existing Questão existente no banco
     * @param updated Questão com novos dados
     * @return true se houver mudanças significativas, false caso contrário
     */
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
                    .collect(java.util.stream.Collectors.toSet());

            Set<Long> updatedOptionIds = updated.getOptions().stream()
                    .map(com.geosegbar.entities.OptionEntity::getId)
                    .collect(java.util.stream.Collectors.toSet());

            if (!existingOptionIds.equals(updatedOptionIds)) {
                log.info("Mudança detectada nas opções da questão {}: {} -> {}",
                        existing.getId(), existingOptionIds, updatedOptionIds);
                return true;
            }
        }

        log.debug("Nenhuma mudança significativa detectada na questão {}", existing.getId());
        return false;
    }

    public QuestionEntity findById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Questão não encontrada!"));
    }

    public List<QuestionEntity> findAll() {
        return questionRepository.findAll();
    }

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
