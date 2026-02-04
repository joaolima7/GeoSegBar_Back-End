package com.geosegbar.infra.template_questionnaire.services;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.checklist.services.ChecklistService;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.question.services.QuestionService;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionDTO;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionnaireCreationDTO;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionnaireUpdateDTO;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateQuestionnaireService {

    private final TemplateQuestionnaireRepository templateQuestionnaireRepository;
    private final ChecklistService checklistService;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final DamRepository damRepository;
    private final ClientRepository clientRepository;
    private final OptionRepository optionRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final AnswerRepository answerRepository;

    @Transactional
    public void deleteById(Long id) {
        log.info("Iniciando exclusão do template {}", id);

        TemplateQuestionnaireEntity template = templateQuestionnaireRepository.findByIdWithFullDetails(id)
                .orElseThrow(() -> new NotFoundException("Template não encontrado para exclusão com ID: " + id));

        boolean hasQuestionnaireResponses = questionnaireResponseRepository.existsByTemplateQuestionnaireId(id);
        if (hasQuestionnaireResponses) {
            throw new BusinessRuleException(
                    "Não é possível excluir o template '" + template.getName()
                    + "' pois existem questionários respondidos associados a ele. "
                    + "A exclusão impactaria na consistência dos dados históricos.");
        }

        log.info("Template {} não possui questionários respondidos. Prosseguindo com exclusão.", id);

        Set<Long> questionIds = template.getTemplateQuestions().stream()
                .map(tq -> tq.getQuestion().getId())
                .collect(Collectors.toSet());

        log.info("Template {} possui {} questões associadas para análise: {}",
                id, questionIds.size(), questionIds);

        templateQuestionnaireRepository.deleteById(id);
        log.info("Template {} e seus relacionamentos TemplateQuestionnaireQuestion deletados.", id);

        int deletedQuestionsCount = 0;
        int keptQuestionsCount = 0;

        for (Long questionId : questionIds) {
            List<AnswerEntity> answers = answerRepository.findByQuestionIdWithDetails(questionId);

            if (answers.isEmpty()) {
                boolean isUsedInOtherTemplates = isQuestionUsedInOtherTemplates(questionId);

                if (!isUsedInOtherTemplates) {
                    try {
                        questionService.deleteById(questionId);
                        deletedQuestionsCount++;
                        log.info("Questão {} deletada: sem answers e não usada em outros templates.", questionId);
                    } catch (Exception e) {
                        log.warn("Não foi possível deletar a questão {}: {}", questionId, e.getMessage());
                        keptQuestionsCount++;
                    }
                } else {
                    keptQuestionsCount++;
                    log.info("Questão {} mantida: usada em outros templates.", questionId);
                }
            } else {
                keptQuestionsCount++;
                log.info("Questão {} mantida: possui {} answer(s) registrada(s).", questionId, answers.size());
            }
        }

        log.info("Exclusão do template {} concluída. Questões deletadas: {}, Questões mantidas: {}",
                id, deletedQuestionsCount, keptQuestionsCount);
    }

    private boolean isQuestionUsedInOtherTemplates(Long questionId) {

        List<TemplateQuestionnaireEntity> templatesWithQuestion
                = templateQuestionnaireRepository.findAllWithFullDetails().stream()
                        .filter(t -> t.getTemplateQuestions().stream()
                        .anyMatch(tq -> tq.getQuestion().getId().equals(questionId)))
                        .collect(Collectors.toList());

        return !templatesWithQuestion.isEmpty();
    }

    @Transactional
    public TemplateQuestionnaireEntity save(TemplateQuestionnaireEntity template) {
        if (template.getDam() == null || template.getDam().getId() == null) {
            throw new InvalidInputException("Template deve estar vinculado a uma barragem.");
        }

        if (templateQuestionnaireRepository.existsByNameAndDamId(template.getName(), template.getDam().getId())) {
            throw new DuplicateResourceException(
                    "Já existe um template com o nome '" + template.getName() + "' para esta barragem.");
        }

        TemplateQuestionnaireEntity saved = templateQuestionnaireRepository.save(template);
        log.info("Template {} criado para barragem {}.", saved.getId(), saved.getDam().getId());
        return saved;
    }

    @Transactional
    public TemplateQuestionnaireEntity update(TemplateQuestionnaireEntity template) {
        TemplateQuestionnaireEntity existing = templateQuestionnaireRepository.findById(template.getId())
                .orElseThrow(() -> new NotFoundException("Template não encontrado para atualização!"));

        if (template.getDam() == null || template.getDam().getId() == null) {
            throw new InvalidInputException("Template deve estar vinculado a uma barragem.");
        }

        if (!existing.getName().equals(template.getName())) {
            if (templateQuestionnaireRepository.existsByNameAndDamIdAndIdNot(
                    template.getName(), template.getDam().getId(), template.getId())) {
                throw new DuplicateResourceException(
                        "Já existe outro template com o nome '" + template.getName() + "' para esta barragem.");
            }
        }

        TemplateQuestionnaireEntity saved = templateQuestionnaireRepository.save(template);
        log.info("Template {} atualizado.", template.getId());
        return saved;
    }

    @Transactional
    public TemplateQuestionnaireEntity createWithQuestions(TemplateQuestionnaireCreationDTO dto) {
        DamEntity dam = damRepository.findById(dto.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + dto.getDamId()));

        if (templateQuestionnaireRepository.existsByNameAndDamId(dto.getName(), dto.getDamId())) {
            throw new DuplicateResourceException(
                    "Já existe um template com o nome '" + dto.getName()
                    + "' para a barragem '" + dam.getName() + "'");
        }

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName(dto.getName());
        template.setDam(dam);
        template.setTemplateQuestions(new HashSet<>());

        template = templateQuestionnaireRepository.save(template);

        for (TemplateQuestionDTO questionDto : dto.getQuestions()) {
            QuestionEntity question;

            if (questionDto.isNewQuestion()) {
                question = createNewQuestion(questionDto, dam.getClient());
                log.info("Nova questão criada com ID: {} para o template: {}", question.getId(), template.getId());
            } else if (questionDto.isExistingQuestion()) {
                question = questionRepository.findById(questionDto.getQuestionId())
                        .orElseThrow(() -> new NotFoundException(
                        "Questão não encontrada com ID: " + questionDto.getQuestionId()));
            } else {
                throw new InvalidInputException("Dados da questão inválidos (ID ou Texto/Tipo obrigatórios).");
            }

            TemplateQuestionnaireQuestionEntity templateQuestion = new TemplateQuestionnaireQuestionEntity();
            templateQuestion.setTemplateQuestionnaire(template);
            templateQuestion.setQuestion(question);
            templateQuestion.setOrderIndex(questionDto.getOrderIndex());

            template.getTemplateQuestions().add(templateQuestion);
        }

        TemplateQuestionnaireEntity saved = templateQuestionnaireRepository.save(template);
        log.info("Template {} criado com questões.", saved.getId());
        return saved;
    }

    @Transactional
    public TemplateQuestionnaireEntity updateWithQuestions(Long templateId, TemplateQuestionnaireUpdateDTO dto) {
        log.info("Iniciando atualização do template {} com questões", templateId);

        TemplateQuestionnaireEntity existingTemplate = templateQuestionnaireRepository.findByIdWithFullDetails(templateId)
                .orElseThrow(() -> new NotFoundException("Template não encontrado com ID: " + templateId));

        DamEntity dam = existingTemplate.getDam();

        if (!existingTemplate.getName().equals(dto.getName())) {
            if (templateQuestionnaireRepository.existsByNameAndDamIdAndIdNot(dto.getName(), dam.getId(), templateId)) {
                throw new DuplicateResourceException(
                        "Já existe outro template com o nome '" + dto.getName()
                        + "' para a barragem '" + dam.getName() + "'");
            }
        }

        existingTemplate.setName(dto.getName());
        existingTemplate.getTemplateQuestions().clear();

        for (TemplateQuestionDTO questionDto : dto.getQuestions()) {
            QuestionEntity question;

            if (questionDto.isNewQuestion()) {
                question = createNewQuestion(questionDto, dam.getClient());
            } else if (questionDto.isExistingQuestion()) {
                question = questionRepository.findById(questionDto.getQuestionId())
                        .orElseThrow(() -> new NotFoundException(
                        "Questão não encontrada com ID: " + questionDto.getQuestionId()));
            } else {
                throw new InvalidInputException("Dados da questão inválidos.");
            }

            TemplateQuestionnaireQuestionEntity templateQuestion = new TemplateQuestionnaireQuestionEntity();
            templateQuestion.setTemplateQuestionnaire(existingTemplate);
            templateQuestion.setQuestion(question);
            templateQuestion.setOrderIndex(questionDto.getOrderIndex());

            existingTemplate.getTemplateQuestions().add(templateQuestion);
        }

        TemplateQuestionnaireEntity saved = templateQuestionnaireRepository.save(existingTemplate);
        log.info("Template {} atualizado com questões.", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public TemplateQuestionnaireEntity findById(Long id) {

        return templateQuestionnaireRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Template não encontrado!"));
    }

    @Transactional(readOnly = true)
    public List<TemplateQuestionnaireEntity> findAll() {
        return templateQuestionnaireRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TemplateQuestionnaireEntity> findByChecklistId(Long checklistId) {

        checklistService.findById(checklistId);

        return templateQuestionnaireRepository.findByChecklistsId(checklistId);
    }

    @Transactional(readOnly = true)
    public List<TemplateQuestionnaireEntity> findByDamIdOrderedByName(Long damId) {
        if (!damRepository.existsById(damId)) {
            throw new NotFoundException("Barragem não encontrada com ID: " + damId);
        }
        return templateQuestionnaireRepository.findByDamIdOrderByNameAsc(damId);
    }

    private QuestionEntity createNewQuestion(TemplateQuestionDTO questionDto, ClientEntity client) {
        if (questionDto.getQuestionText() == null || questionDto.getQuestionText().isBlank()) {
            throw new InvalidInputException("Texto da questão é obrigatório!");
        }
        if (questionDto.getType() == null) {
            throw new InvalidInputException("Tipo da questão é obrigatório!");
        }

        if (questionDto.getType().name().equals("CHECKBOX")) {
            if (questionDto.getOptionIds() == null || questionDto.getOptionIds().isEmpty()) {
                throw new InvalidInputException("Questões CHECKBOX devem ter opções!");
            }
        } else if (questionDto.getType().name().equals("TEXT")) {
            if (questionDto.getOptionIds() != null && !questionDto.getOptionIds().isEmpty()) {
                throw new InvalidInputException("Questões TEXT não devem ter opções!");
            }
        }

        ClientEntity questionClient = client;
        if (questionDto.getClientId() != null) {
            questionClient = clientRepository.findById(questionDto.getClientId())
                    .orElseThrow(() -> new NotFoundException("Cliente não encontrado!"));
        }

        QuestionEntity newQuestion = new QuestionEntity();
        newQuestion.setQuestionText(questionDto.getQuestionText());
        newQuestion.setType(questionDto.getType());
        newQuestion.setClient(questionClient);

        if (questionDto.getOptionIds() != null && !questionDto.getOptionIds().isEmpty()) {
            Set<OptionEntity> options = new HashSet<>();
            for (Long optionId : questionDto.getOptionIds()) {
                OptionEntity option = optionRepository.findById(optionId)
                        .orElseThrow(() -> new NotFoundException("Opção não encontrada: " + optionId));
                options.add(option);
            }
            newQuestion.setOptions(options);
        } else {
            newQuestion.setOptions(new HashSet<>());
        }

        return questionService.save(newQuestion);
    }

    @Transactional
    public TemplateQuestionnaireEntity replicateTemplate(Long sourceTemplateId, Long targetDamId) {
        log.info("Iniciando replicação do template {} para a barragem {}", sourceTemplateId, targetDamId);

        TemplateQuestionnaireEntity sourceTemplate = templateQuestionnaireRepository
                .findByIdWithFullDetails(sourceTemplateId)
                .orElseThrow(() -> new NotFoundException("Template de origem não encontrado!"));

        DamEntity targetDam = damRepository.findById(targetDamId)
                .orElseThrow(() -> new NotFoundException("Barragem de destino não encontrada!"));

        DamEntity sourceDam = sourceTemplate.getDam();
        if (!sourceDam.getClient().getId().equals(targetDam.getClient().getId())) {
            throw new BusinessRuleException("Não é possível replicar template entre clientes diferentes.");
        }

        String templateName = sourceTemplate.getName();
        if (templateQuestionnaireRepository.existsByNameAndDamId(templateName, targetDamId)) {
            throw new DuplicateResourceException("Já existe um template com o nome '" + templateName + "' na barragem de destino.");
        }

        TemplateQuestionnaireEntity newTemplate = new TemplateQuestionnaireEntity();
        newTemplate.setName(sourceTemplate.getName());
        newTemplate.setDam(targetDam);
        newTemplate.setTemplateQuestions(new HashSet<>());

        newTemplate = templateQuestionnaireRepository.save(newTemplate);

        List<TemplateQuestionnaireQuestionEntity> sortedQuestions = sourceTemplate.getTemplateQuestions()
                .stream()
                .sorted(Comparator.comparing(TemplateQuestionnaireQuestionEntity::getOrderIndex))
                .collect(Collectors.toList());

        for (TemplateQuestionnaireQuestionEntity sourceTemplateQuestion : sortedQuestions) {
            QuestionEntity existingQuestion = sourceTemplateQuestion.getQuestion();

            TemplateQuestionnaireQuestionEntity newTemplateQuestion = new TemplateQuestionnaireQuestionEntity();
            newTemplateQuestion.setTemplateQuestionnaire(newTemplate);
            newTemplateQuestion.setQuestion(existingQuestion);
            newTemplateQuestion.setOrderIndex(sourceTemplateQuestion.getOrderIndex());

            newTemplate.getTemplateQuestions().add(newTemplateQuestion);
        }

        newTemplate = templateQuestionnaireRepository.save(newTemplate);
        return newTemplate;
    }
}
