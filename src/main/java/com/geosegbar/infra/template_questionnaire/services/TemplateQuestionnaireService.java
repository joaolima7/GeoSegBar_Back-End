package com.geosegbar.infra.template_questionnaire.services;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

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

import jakarta.transaction.Transactional;
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

        // 1. Busca o template com todos os detalhes
        TemplateQuestionnaireEntity template = templateQuestionnaireRepository.findByIdWithFullDetails(id)
                .orElseThrow(() -> new NotFoundException("Template não encontrado para exclusão com ID: " + id));

        // 2. Verifica se existe QuestionnaireResponse associado ao template
        boolean hasQuestionnaireResponses = questionnaireResponseRepository.existsByTemplateQuestionnaireId(id);
        if (hasQuestionnaireResponses) {
            throw new BusinessRuleException(
                    "Não é possível excluir o template '" + template.getName()
                    + "' pois existem questionários respondidos associados a ele. "
                    + "A exclusão impactaria na consistência dos dados históricos.");
        }

        log.info("Template {} não possui questionários respondidos. Prosseguindo com exclusão.", id);

        // 3. Coleta todas as questões do template para análise posterior
        Set<Long> questionIds = template.getTemplateQuestions().stream()
                .map(tq -> tq.getQuestion().getId())
                .collect(Collectors.toSet());

        log.info("Template {} possui {} questões associadas para análise: {}",
                id, questionIds.size(), questionIds);

        // 4. Deleta o template (cascade irá deletar automaticamente os TemplateQuestionnaireQuestion)
        templateQuestionnaireRepository.deleteById(id);
        log.info("Template {} e seus relacionamentos TemplateQuestionnaireQuestion deletados.", id);

        // 5. Para cada questão, verifica se ela não tem nenhuma Answer registrada
        // Se não tiver nenhuma Answer, a questão pode ser deletada
        int deletedQuestionsCount = 0;
        int keptQuestionsCount = 0;

        for (Long questionId : questionIds) {
            // Verifica se a questão tem answers
            List<AnswerEntity> answers = answerRepository.findByQuestionIdWithDetails(questionId);

            if (answers.isEmpty()) {
                // Verifica se a questão não está sendo usada em outros templates
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

    /**
     * Verifica se uma questão está sendo usada em outros templates.
     *
     * @param questionId ID da questão a verificar
     * @return true se a questão está em uso em outros templates, false caso
     * contrário
     */
    private boolean isQuestionUsedInOtherTemplates(Long questionId) {
        // Busca todos os templates que contêm esta questão
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
        log.info("Template {} criado para barragem {}.",
                saved.getId(), saved.getDam().getId());

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

            // Verifica se é para criar nova questão ou usar existente
            if (questionDto.isNewQuestion()) {
                question = createNewQuestion(questionDto, dam.getClient());
                log.info("Nova questão criada com ID: {} para o template: {}", question.getId(), template.getId());
            } else if (questionDto.isExistingQuestion()) {
                question = questionRepository.findById(questionDto.getQuestionId())
                        .orElseThrow(() -> new NotFoundException(
                        "Questão não encontrada com ID: " + questionDto.getQuestionId()));
            } else {
                throw new InvalidInputException(
                        "É necessário informar questionId (para usar questão existente) "
                        + "ou questionText + type (para criar nova questão)!");
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

        // Busca o template existente
        TemplateQuestionnaireEntity existingTemplate = templateQuestionnaireRepository.findByIdWithFullDetails(templateId)
                .orElseThrow(() -> new NotFoundException("Template não encontrado com ID: " + templateId));

        DamEntity dam = existingTemplate.getDam();

        // Valida se o nome foi alterado e se já existe outro template com o mesmo nome
        if (!existingTemplate.getName().equals(dto.getName())) {
            if (templateQuestionnaireRepository.existsByNameAndDamIdAndIdNot(dto.getName(), dam.getId(), templateId)) {
                throw new DuplicateResourceException(
                        "Já existe outro template com o nome '" + dto.getName()
                        + "' para a barragem '" + dam.getName() + "'");
            }
        }

        // Atualiza o nome do template
        existingTemplate.setName(dto.getName());

        // Remove todas as questões antigas
        existingTemplate.getTemplateQuestions().clear();

        // Processa as novas questões
        for (TemplateQuestionDTO questionDto : dto.getQuestions()) {
            QuestionEntity question;

            // Verifica se é para criar nova questão ou usar existente
            if (questionDto.isNewQuestion()) {
                question = createNewQuestion(questionDto, dam.getClient());
                log.info("Nova questão criada com ID: {} durante atualização do template: {}",
                        question.getId(), templateId);
            } else if (questionDto.isExistingQuestion()) {
                question = questionRepository.findById(questionDto.getQuestionId())
                        .orElseThrow(() -> new NotFoundException(
                        "Questão não encontrada com ID: " + questionDto.getQuestionId()));
            } else {
                throw new InvalidInputException(
                        "É necessário informar questionId (para usar questão existente) "
                        + "ou questionText + type (para criar nova questão)!");
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

    public List<TemplateQuestionnaireEntity> findByDamIdOrderedByName(Long damId) {
        if (!damRepository.existsById(damId)) {
            throw new NotFoundException("Barragem não encontrada com ID: " + damId);
        }
        return templateQuestionnaireRepository.findByDamIdOrderByNameAsc(damId);
    }

    /**
     * Cria uma nova questão com base nos dados do DTO. Usado no método
     * createWithQuestions para criar questões que ainda não existem.
     *
     * @param questionDto DTO com dados da questão a ser criada
     * @param client Cliente associado à questão
     * @return QuestionEntity criada e salva
     */
    private QuestionEntity createNewQuestion(TemplateQuestionDTO questionDto, ClientEntity client) {
        // Validações
        if (questionDto.getQuestionText() == null || questionDto.getQuestionText().isBlank()) {
            throw new InvalidInputException("Texto da questão é obrigatório para criar nova questão!");
        }

        if (questionDto.getType() == null) {
            throw new InvalidInputException("Tipo da questão é obrigatório para criar nova questão!");
        }

        // Validação específica por tipo
        if (questionDto.getType().name().equals("CHECKBOX")) {
            if (questionDto.getOptionIds() == null || questionDto.getOptionIds().isEmpty()) {
                throw new InvalidInputException(
                        "Questões do tipo CHECKBOX devem ter pelo menos uma opção associada!");
            }
        } else if (questionDto.getType().name().equals("TEXT")) {
            if (questionDto.getOptionIds() != null && !questionDto.getOptionIds().isEmpty()) {
                throw new InvalidInputException(
                        "Questões do tipo TEXT não devem ter opções associadas!");
            }
        }

        // Se clientId foi fornecido, usa ele; caso contrário, usa o cliente da barragem
        ClientEntity questionClient = client;
        if (questionDto.getClientId() != null) {
            questionClient = clientRepository.findById(questionDto.getClientId())
                    .orElseThrow(() -> new NotFoundException(
                    "Cliente não encontrado com ID: " + questionDto.getClientId()));
        }

        // Cria a questão
        QuestionEntity newQuestion = new QuestionEntity();
        newQuestion.setQuestionText(questionDto.getQuestionText());
        newQuestion.setType(questionDto.getType());
        newQuestion.setClient(questionClient);

        // Associa as opções se fornecidas
        if (questionDto.getOptionIds() != null && !questionDto.getOptionIds().isEmpty()) {
            Set<OptionEntity> options = new HashSet<>();
            for (Long optionId : questionDto.getOptionIds()) {
                OptionEntity option = optionRepository.findById(optionId)
                        .orElseThrow(() -> new NotFoundException(
                        "Opção não encontrada com ID: " + optionId));
                options.add(option);
            }
            newQuestion.setOptions(options);
        } else {
            newQuestion.setOptions(new HashSet<>());
        }

        // Salva usando o serviço para manter todas as validações e cache
        return questionService.save(newQuestion);
    }

    /**
     * Replica um template de questionário de uma barragem para outra. Cria um
     * novo template para a barragem de destino, mas reutiliza as questões
     * existentes, já que questões pertencem ao cliente e são compartilhadas
     * entre templates.
     *
     * IMPORTANTE: Esta operação NÃO associa o template a nenhum checklist. Para
     * replicar templates E associá-los a um checklist, use replicateChecklist.
     *
     * @param sourceTemplateId ID do template de origem
     * @param targetDamId ID da barragem de destino
     * @return Template replicado reutilizando as mesmas questões
     */
    @Transactional
    public TemplateQuestionnaireEntity replicateTemplate(Long sourceTemplateId, Long targetDamId) {
        log.info("Iniciando replicação do template {} para a barragem {}", sourceTemplateId, targetDamId);

        TemplateQuestionnaireEntity sourceTemplate = templateQuestionnaireRepository
                .findByIdWithFullDetails(sourceTemplateId)
                .orElseThrow(() -> new NotFoundException(
                "Template de origem não encontrado com ID: " + sourceTemplateId));

        DamEntity targetDam = damRepository.findById(targetDamId)
                .orElseThrow(() -> new NotFoundException(
                "Barragem de destino não encontrada com ID: " + targetDamId));

        DamEntity sourceDam = sourceTemplate.getDam();
        if (!sourceDam.getClient().getId().equals(targetDam.getClient().getId())) {
            throw new BusinessRuleException(
                    "Não é possível replicar template entre barragens de clientes diferentes. "
                    + "Barragem de origem pertence ao cliente '" + sourceDam.getClient().getName()
                    + "' e barragem de destino pertence ao cliente '" + targetDam.getClient().getName() + "'.");
        }

        String templateName = sourceTemplate.getName();
        if (templateQuestionnaireRepository.existsByNameAndDamId(templateName, targetDamId)) {
            throw new DuplicateResourceException(
                    "Já existe um template com o nome '" + templateName
                    + "' na barragem de destino. Escolha outro template ou renomeie o existente.");
        }

        log.info("Validações concluídas. Iniciando criação do template replicado...");

        TemplateQuestionnaireEntity newTemplate = new TemplateQuestionnaireEntity();
        newTemplate.setName(sourceTemplate.getName());
        newTemplate.setDam(targetDam);
        newTemplate.setTemplateQuestions(new HashSet<>());

        newTemplate = templateQuestionnaireRepository.save(newTemplate);
        log.info("Template replicado criado com ID: {}", newTemplate.getId());

        List<TemplateQuestionnaireQuestionEntity> sortedQuestions = sourceTemplate.getTemplateQuestions()
                .stream()
                .sorted(Comparator.comparing(TemplateQuestionnaireQuestionEntity::getOrderIndex))
                .collect(Collectors.toList());

        int questionCount = 0;
        for (TemplateQuestionnaireQuestionEntity sourceTemplateQuestion : sortedQuestions) {
            QuestionEntity existingQuestion = sourceTemplateQuestion.getQuestion();

            TemplateQuestionnaireQuestionEntity newTemplateQuestion = new TemplateQuestionnaireQuestionEntity();
            newTemplateQuestion.setTemplateQuestionnaire(newTemplate);
            newTemplateQuestion.setQuestion(existingQuestion);
            newTemplateQuestion.setOrderIndex(sourceTemplateQuestion.getOrderIndex());

            newTemplate.getTemplateQuestions().add(newTemplateQuestion);
            questionCount++;
        }

        newTemplate = templateQuestionnaireRepository.save(newTemplate);

        log.info("Replicação concluída: Template {} criado reutilizando {} questão(ões) para barragem {} (SEM associação a checklist)",
                newTemplate.getId(), questionCount, targetDamId);

        return newTemplate;
    }
}
