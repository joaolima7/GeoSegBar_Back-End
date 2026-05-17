package com.geosegbar.infra.checklist.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.AssociationAction;
import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.ChecklistTemplateEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.checklist.dtos.ChecklistCompleteCreationDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistCompleteDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistCompleteUpdateDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistNameResponseDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistNameUpdateDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistTemplateAssociationDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistTemplateAssociationResponseDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersAndDamDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersDTO;
import com.geosegbar.infra.checklist.dtos.OptionDTO;
import com.geosegbar.infra.checklist.dtos.QuestionWithLastAnswerDTO;
import com.geosegbar.infra.checklist.dtos.TemplateInChecklistDTO;
import com.geosegbar.infra.checklist.dtos.TemplateOrderDTO;
import com.geosegbar.infra.checklist.dtos.TemplateQuestionnaireWithAnswersDTO;
import com.geosegbar.infra.checklist.dtos.TemplateReorderDTO;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist_template.persistence.jpa.ChecklistTemplateRepository;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.question.services.QuestionService;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionDTO;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final DamService damService;
    private final AnswerRepository answerRepository;
    private final TemplateQuestionnaireRepository templateQuestionnaireRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final OptionRepository optionRepository;

    public Page<ChecklistEntity> findAllPaged(Pageable pageable) {
        return checklistRepository.findAllWithDams(pageable);
    }

    public ChecklistCompleteDTO findByIdDTO(Long id) {
        ChecklistEntity entity = checklistRepository.findByIdWithFullDetails(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrado para id: " + id));
        return convertToCompleteDTO(entity);
    }

    public ChecklistEntity findById(Long id) {
        return checklistRepository.findByIdWithFullDetails(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para id: " + id));
    }

    @Transactional
    public ChecklistEntity save(ChecklistEntity checklist) {

        String actor = resolveActor();
        logChecklistAudit(
                "CHECKLIST_CREATE",
                "START",
                actor,
                "damId=" + (checklist.getDam() != null ? checklist.getDam().getId() : null)
                + " name=" + checklist.getName() + " "
                + templateIdsSummary(checklist.getChecklistTemplates()),
                null
        );

        try {
            if (checklist.getDam() == null) {
                throw new InvalidInputException("Checklist deve estar vinculado a uma barragem.");
            }

            DamEntity dam = checklist.getDam();
            Long damId = dam.getId();

            DamEntity fullDam = damService.findById(damId);

            ChecklistEntity existingChecklist = checklistRepository.findByDamId(damId);
            if (existingChecklist != null) {
                throw new BusinessRuleException(
                        "Não é possível criar um novo checklist para esta barragem. "
                        + "A barragem '" + fullDam.getName() + "' já possui um checklist cadastrado. "
                        + "Edite o checklist existente ao invés de criar um novo."
                );
            }

            if (checklistRepository.existsByNameAndDamId(checklist.getName(), damId)) {
                throw new DuplicateResourceException("Já existe um checklist com esse nome para esta barragem.");
            }

            validateTemplatesBelongToDam(checklist.getTemplateQuestionnairesForJson(), damId, fullDam.getName());

            ChecklistEntity saved = checklistRepository.save(checklist);
            ChecklistEntity result = findById(saved.getId());

            logChecklistAudit(
                    "CHECKLIST_CREATE",
                    "SUCCESS",
                    actor,
                    "checklistId=" + result.getId() + " damId=" + damId
                    + " templateCount=" + result.getChecklistTemplates().size(),
                    null
            );

            return result;
        } catch (RuntimeException e) {
            logChecklistAudit(
                    "CHECKLIST_CREATE",
                    "ERROR",
                    actor,
                    "name=" + checklist.getName(),
                    e
            );
            throw e;
        }
    }

    @Transactional()
    public List<ChecklistWithLastAnswersDTO> findChecklistsWithLastAnswersForDam(Long damId) {
        damService.findById(damId);

        ChecklistEntity checklist = checklistRepository.findByDamIdWithFullDetails(damId);

        List<ChecklistWithLastAnswersDTO> result = new ArrayList<>();

        if (checklist == null) {
            return result;
        }

        result.add(buildChecklistWithAnswersDTO(checklist, damId));

        return result;
    }

    @Transactional()
    public List<ChecklistWithLastAnswersAndDamDTO> findAllChecklistsWithLastAnswersByClientId(Long clientId) {

        List<ChecklistEntity> allChecklistsEntities = checklistRepository.findAllByClientIdWithDetails(clientId);

        List<ChecklistWithLastAnswersAndDamDTO> allChecklists = new ArrayList<>();

        for (ChecklistEntity checklist : allChecklistsEntities) {

            ChecklistWithLastAnswersAndDamDTO checklistDTO = new ChecklistWithLastAnswersAndDamDTO();
            checklistDTO.setId(checklist.getId());
            checklistDTO.setName(checklist.getName());
            checklistDTO.setCreatedAt(checklist.getCreatedAt());

            DamEntity dam = checklist.getDam();
            ChecklistWithLastAnswersAndDamDTO.DamInfoDTO damInfo = new ChecklistWithLastAnswersAndDamDTO.DamInfoDTO(
                    dam.getId(),
                    dam.getName(),
                    dam.getCity(),
                    dam.getState(),
                    dam.getLatitude(),
                    dam.getLongitude()
            );
            checklistDTO.setDam(damInfo);

            populateTemplatesWithAnswers(checklistDTO, checklist, dam.getId());

            allChecklists.add(checklistDTO);
        }

        allChecklists.sort((a, b) -> {
            int damComparison = a.getDam().getName().compareTo(b.getDam().getName());
            return damComparison != 0 ? damComparison : a.getName().compareTo(b.getName());
        });

        return allChecklists;
    }

    private ChecklistWithLastAnswersDTO buildChecklistWithAnswersDTO(ChecklistEntity checklist, Long damId) {
        ChecklistWithLastAnswersDTO checklistDTO = new ChecklistWithLastAnswersDTO();
        checklistDTO.setId(checklist.getId());
        checklistDTO.setName(checklist.getName());
        checklistDTO.setCreatedAt(checklist.getCreatedAt());

        populateTemplatesWithAnswers(checklistDTO, checklist, damId);

        return checklistDTO;
    }

    private void populateTemplatesWithAnswers(Object checklistDTO, ChecklistEntity checklist, Long damId) {
        List<TemplateQuestionnaireWithAnswersDTO> templateDTOs = new ArrayList<>();

        Set<Long> seenCtIds = new HashSet<>();
        for (ChecklistTemplateEntity ct : checklist.getChecklistTemplates()) {
            if (!seenCtIds.add(ct.getId())) continue;
            TemplateQuestionnaireEntity template = ct.getTemplateQuestionnaire();

            TemplateQuestionnaireWithAnswersDTO templateDTO = new TemplateQuestionnaireWithAnswersDTO();
            templateDTO.setId(template.getId());
            templateDTO.setName(template.getName());
            templateDTO.setOrderIndex(ct.getOrderIndex());

            List<QuestionWithLastAnswerDTO> questionDTOs = new ArrayList<>();

            for (TemplateQuestionnaireQuestionEntity tqQuestion : template.getTemplateQuestions()) {
                QuestionEntity question = tqQuestion.getQuestion();

                QuestionWithLastAnswerDTO questionDTO = new QuestionWithLastAnswerDTO();
                questionDTO.setId(question.getId());
                questionDTO.setQuestionText(question.getQuestionText());
                questionDTO.setType(question.getType());

                List<OptionDTO> allOptionDTOs = question.getOptions().stream()
                        .map(opt -> new OptionDTO(opt.getId(), opt.getLabel(), opt.getValue()))
                        .collect(Collectors.toList());
                questionDTO.setAllOptions(allOptionDTOs);

                Optional<AnswerEntity> lastNonNIAnswer = answerRepository.findLatestNonNIAnswer(
                        damId, question.getId(), template.getId());

                lastNonNIAnswer.ifPresent(answer -> {
                    Optional<OptionEntity> nonNIOption = answer.getSelectedOptions().stream()
                            .filter(opt -> !"NI".equalsIgnoreCase(opt.getLabel()))
                            .findFirst();

                    nonNIOption.ifPresent(option -> {
                        OptionDTO optionDTO = new OptionDTO(
                                option.getId(), option.getLabel(), option.getValue());
                        questionDTO.setLastSelectedOption(optionDTO);
                        questionDTO.setAnswerResponseId(answer.getId());
                    });
                });

                questionDTOs.add(questionDTO);
            }

            templateDTO.setQuestions(questionDTOs);
            templateDTOs.add(templateDTO);
        }

        if (checklistDTO instanceof ChecklistWithLastAnswersDTO) {
            ((ChecklistWithLastAnswersDTO) checklistDTO).setTemplateQuestionnaires(templateDTOs);
        } else if (checklistDTO instanceof ChecklistWithLastAnswersAndDamDTO) {
            ((ChecklistWithLastAnswersAndDamDTO) checklistDTO).setTemplateQuestionnaires(templateDTOs);
        }
    }

    @Transactional
    public ChecklistNameResponseDTO updateName(Long checklistId, ChecklistNameUpdateDTO dto) {
        String actor = resolveActor();
        String newName = dto.getName().trim();

        logChecklistAudit(
                "CHECKLIST_UPDATE_NAME",
                "START",
                actor,
                "checklistId=" + checklistId + " name=" + newName,
                null
        );

        try {
            ChecklistEntity checklist = checklistRepository.findByIdWithDam(checklistId)
                    .orElseThrow(() -> new NotFoundException("Checklist não encontrada para id: " + checklistId));

            Long damId = checklist.getDam().getId();
            if (checklistRepository.existsByNameAndDamIdAndIdNot(newName, damId, checklistId)) {
                throw new DuplicateResourceException("Já existe um checklist com esse nome para esta barragem.");
            }

            checklist.setName(newName);
            ChecklistEntity saved = checklistRepository.save(checklist);

            logChecklistAudit(
                    "CHECKLIST_UPDATE_NAME",
                    "SUCCESS",
                    actor,
                    "checklistId=" + saved.getId() + " damId=" + damId,
                    null
            );

            return new ChecklistNameResponseDTO(saved.getId(), saved.getName(), damId);
        } catch (RuntimeException e) {
            logChecklistAudit(
                    "CHECKLIST_UPDATE_NAME",
                    "ERROR",
                    actor,
                    "checklistId=" + checklistId + " name=" + newName,
                    e
            );
            throw e;
        }
    }

    @Transactional
    public ChecklistEntity update(ChecklistEntity checklist) {

        String actor = resolveActor();
        logChecklistAudit(
                "CHECKLIST_UPDATE",
                "START",
                actor,
                "checklistId=" + checklist.getId() + " " + templateIdsSummary(checklist.getChecklistTemplates()),
                null
        );

        try {
            if (checklist.getDam() == null) {
                throw new InvalidInputException("Checklist deve estar vinculado a uma barragem.");
            }

            ChecklistEntity oldChecklist = findById(checklist.getId());
            DamEntity oldDam = oldChecklist.getDam();
            Long oldDamId = oldDam.getId();
            DamEntity oldFullDam = damService.findById(oldDamId);

            DamEntity newDam = checklist.getDam();
            Long newDamId = newDam.getId();

            if (!oldDamId.equals(newDamId)) {
                throw new BusinessRuleException(
                        "Não é possível alterar a barragem de um checklist após sua criação. "
                        + "O checklist '" + oldChecklist.getName() + "' está vinculado à barragem '"
                        + oldFullDam.getName() + "' e não pode ser transferido para outra barragem."
                );
            }

            if (checklistRepository.existsByNameAndDamIdAndIdNot(checklist.getName(), newDamId, checklist.getId())) {
                throw new DuplicateResourceException("Já existe um checklist com esse nome para esta barragem.");
            }

            validateTemplatesBelongToDam(checklist.getTemplateQuestionnairesForJson(), newDamId, oldFullDam.getName());

            ChecklistEntity saved = checklistRepository.save(checklist);
            ChecklistEntity result = findById(saved.getId());

            logChecklistAudit(
                    "CHECKLIST_UPDATE",
                    "SUCCESS",
                    actor,
                    "checklistId=" + result.getId() + " templateCount=" + result.getChecklistTemplates().size(),
                    null
            );

            return result;
        } catch (RuntimeException e) {
            logChecklistAudit(
                    "CHECKLIST_UPDATE",
                    "ERROR",
                    actor,
                    "checklistId=" + checklist.getId(),
                    e
            );
            throw e;
        }
    }

    @Transactional
    public void deleteById(Long id) {
        String actor = resolveActor();
        logChecklistAudit(
                "CHECKLIST_DELETE",
                "START",
                actor,
                "checklistId=" + id,
                null
        );

        try {
            if (!checklistRepository.existsById(id)) {
                throw new NotFoundException("Checklist não encontrada para exclusão!");
            }
            checklistRepository.deleteById(id);

            logChecklistAudit(
                    "CHECKLIST_DELETE",
                    "SUCCESS",
                    actor,
                    "checklistId=" + id,
                    null
            );
        } catch (RuntimeException e) {
            logChecklistAudit(
                    "CHECKLIST_DELETE",
                    "ERROR",
                    actor,
                    "checklistId=" + id,
                    e
            );
            throw e;
        }
    }

    @Transactional
    public ChecklistEntity createComplete(ChecklistCompleteCreationDTO dto) {
        String actor = resolveActor();
        logChecklistAudit(
                "CHECKLIST_CREATE_COMPLETE",
                "START",
                actor,
                "damId=" + dto.getDamId() + " name=" + dto.getName() + " " + templateDtoSummary(dto.getTemplates()),
                null
        );

        log.info("Iniciando criação de checklist completo: {}", dto.getName());

        try {

            DamEntity dam = damService.findById(dto.getDamId());
            Long clientId = dam.getClient().getId();

            ChecklistEntity existingChecklist = checklistRepository.findByDamId(dto.getDamId());
            if (existingChecklist != null) {
                throw new BusinessRuleException(
                        "Não é possível criar um novo checklist para esta barragem. "
                        + "A barragem '" + dam.getName() + "' já possui um checklist cadastrado. "
                        + "Edite o checklist existente ao invés de criar um novo."
                );
            }

            if (checklistRepository.existsByNameAndDamId(dto.getName(), dto.getDamId())) {
                throw new DuplicateResourceException("Já existe um checklist com esse nome para esta barragem.");
            }

            ChecklistEntity checklist = new ChecklistEntity();
            checklist.setName(dto.getName());
            checklist.setDam(dam);

            checklist = checklistRepository.save(checklist);
            log.info("Checklist base criado com ID: {}", checklist.getId());

            int templateCount = 0;
            for (TemplateInChecklistDTO templateDto : dto.getTemplates()) {
                if (templateDto.isNewTemplate()) {
                    if (templateDto.getName() == null || templateDto.getName().trim().isEmpty()) {
                        throw new InvalidInputException("Nome do template é obrigatório ao criar um novo template!");
                    }
                    if (templateDto.getQuestions() == null || templateDto.getQuestions().isEmpty()) {
                        throw new InvalidInputException("Template deve ter pelo menos uma questão ao criar um novo template!");
                    }
                } else if (templateDto.isExistingTemplate()) {
                    if (templateDto.getTemplateId() == null) {
                        throw new InvalidInputException("ID do template é obrigatório ao usar um template existente!");
                    }
                } else {
                    throw new InvalidInputException("É necessário informar templateId (para template existente) ou name + questions (para criar novo)!");
                }

                TemplateQuestionnaireEntity template;

                if (templateDto.isExistingTemplate()) {
                    template = templateQuestionnaireRepository.findById(templateDto.getTemplateId())
                            .orElseThrow(() -> new NotFoundException(
                            "Template não encontrado com ID: " + templateDto.getTemplateId()));

                    if (!template.getDam().getId().equals(dto.getDamId())) {
                        String templateDamName = damService.findById(template.getDam().getId()).getName();
                        throw new BusinessRuleException(
                                "O template '" + template.getName() + "' pertence à barragem '"
                                + templateDamName + "', mas o checklist está vinculado à barragem '"
                                + dam.getName() + "'. Todos os templates devem pertencer à mesma barragem."
                        );
                    }
                    log.info("Reutilizando template existente: {} (ID: {})", template.getName(), template.getId());
                } else {
                    template = createNewTemplate(templateDto, dam, clientId);
                    log.info("Novo template criado: {} (ID: {})", template.getName(), template.getId());
                }

                int orderIdx = templateDto.getOrderIndex() != null ? templateDto.getOrderIndex() : templateCount + 1;
                ChecklistTemplateEntity ct = new ChecklistTemplateEntity();
                ct.setChecklist(checklist);
                ct.setTemplateQuestionnaire(template);
                ct.setOrderIndex(orderIdx);
                checklist.getChecklistTemplates().add(ct);
                templateCount++;
            }

            ChecklistEntity saved = checklistRepository.save(checklist);

            log.info("Checklist completo criado com sucesso: {} com {} template(s)",
                    saved.getName(), templateCount);

            ChecklistEntity result = findById(saved.getId());

            logChecklistAudit(
                    "CHECKLIST_CREATE_COMPLETE",
                    "SUCCESS",
                    actor,
                    "checklistId=" + result.getId() + " damId=" + dto.getDamId()
                    + " templateCount=" + result.getChecklistTemplates().size(),
                    null
            );

            return result;
        } catch (RuntimeException e) {
            logChecklistAudit(
                    "CHECKLIST_CREATE_COMPLETE",
                    "ERROR",
                    actor,
                    "damId=" + dto.getDamId() + " name=" + dto.getName(),
                    e
            );
            throw e;
        }
    }

    @Transactional
    public ChecklistEntity updateComplete(Long checklistId, ChecklistCompleteUpdateDTO dto) {
        String actor = resolveActor();
        logChecklistAudit(
                "CHECKLIST_UPDATE_COMPLETE",
                "START",
                actor,
                "checklistId=" + checklistId + " name=" + dto.getName() + " " + templateDtoSummary(dto.getTemplates()),
                null
        );

        log.info("Iniciando atualização completa do checklist {}", checklistId);

        try {

            ChecklistEntity existingChecklist = findById(checklistId);
            DamEntity dam = existingChecklist.getDam();
            Long damId = dam.getId();
            Long clientId = dam.getClient().getId();

            if (checklistRepository.existsByNameAndDamIdAndIdNot(dto.getName(), damId, checklistId)) {
                throw new DuplicateResourceException("Já existe um checklist com esse nome para esta barragem.");
            }

            existingChecklist.setName(dto.getName());
            existingChecklist.getChecklistTemplates().clear();

            int templateCount = 0;
            for (TemplateInChecklistDTO templateDto : dto.getTemplates()) {
                if (templateDto.isNewTemplate()) {
                    if (templateDto.getName() == null || templateDto.getName().trim().isEmpty()) {
                        throw new InvalidInputException("Nome do template é obrigatório ao criar um novo template!");
                    }
                    if (templateDto.getQuestions() == null || templateDto.getQuestions().isEmpty()) {
                        throw new InvalidInputException("Template deve ter pelo menos uma questão ao criar um novo template!");
                    }
                } else if (templateDto.isExistingTemplate()) {
                    if (templateDto.getTemplateId() == null) {
                        throw new InvalidInputException("ID do template é obrigatório ao usar um template existente!");
                    }
                } else {
                    throw new InvalidInputException("É necessário informar templateId (para template existente) ou name + questions (para criar novo)!");
                }

                TemplateQuestionnaireEntity template;

                if (templateDto.isExistingTemplate()) {
                    template = templateQuestionnaireRepository.findById(templateDto.getTemplateId())
                            .orElseThrow(() -> new NotFoundException(
                            "Template não encontrado com ID: " + templateDto.getTemplateId()));

                    if (!template.getDam().getId().equals(damId)) {
                        String templateDamName = damService.findById(template.getDam().getId()).getName();
                        throw new BusinessRuleException(
                                "O template '" + template.getName() + "' pertence à barragem '"
                                + templateDamName + "', mas o checklist está vinculado à barragem '"
                                + dam.getName() + "'. Todos os templates devem pertencer à mesma barragem."
                        );
                    }
                    log.info("Reutilizando template existente: {} (ID: {})", template.getName(), template.getId());
                } else {
                    template = createNewTemplate(templateDto, dam, clientId);
                    log.info("Novo template criado: {} (ID: {})", template.getName(), template.getId());
                }

                int orderIdx = templateDto.getOrderIndex() != null ? templateDto.getOrderIndex() : templateCount + 1;
                ChecklistTemplateEntity ct = new ChecklistTemplateEntity();
                ct.setChecklist(existingChecklist);
                ct.setTemplateQuestionnaire(template);
                ct.setOrderIndex(orderIdx);
                existingChecklist.getChecklistTemplates().add(ct);
                templateCount++;
            }

            ChecklistEntity saved = checklistRepository.save(existingChecklist);

            log.info("Checklist {} atualizado com sucesso com {} template(s)",
                    saved.getName(), templateCount);

            ChecklistEntity result = findById(saved.getId());

            logChecklistAudit(
                    "CHECKLIST_UPDATE_COMPLETE",
                    "SUCCESS",
                    actor,
                    "checklistId=" + result.getId() + " templateCount=" + result.getChecklistTemplates().size(),
                    null
            );

            return result;
        } catch (RuntimeException e) {
            logChecklistAudit(
                    "CHECKLIST_UPDATE_COMPLETE",
                    "ERROR",
                    actor,
                    "checklistId=" + checklistId + " name=" + dto.getName(),
                    e
            );
            throw e;
        }
    }

    private TemplateQuestionnaireEntity createNewTemplate(
            TemplateInChecklistDTO templateDto, DamEntity dam, Long clientId) {

        if (templateQuestionnaireRepository.existsByNameAndDamId(templateDto.getName(), dam.getId())) {
            throw new DuplicateResourceException(
                    "Já existe um template com o nome '" + templateDto.getName()
                    + "' para a barragem '" + dam.getName() + "'");
        }

        validateNoDuplicateQuestionIds(templateDto.getQuestions(), templateDto.getName());

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName(templateDto.getName());
        template.setDam(dam);
        template.setTemplateQuestions(new HashSet<>());

        template = templateQuestionnaireRepository.save(template);

        for (TemplateQuestionDTO questionDto : templateDto.getQuestions()) {
            QuestionEntity question;

            if (questionDto.isNewQuestion()) {
                question = createNewQuestion(questionDto, clientId);
                log.info("Nova questão criada com ID: {} para o template: {}",
                        question.getId(), template.getId());
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

        return templateQuestionnaireRepository.save(template);
    }

    private void validateNoDuplicateQuestionIds(List<TemplateQuestionDTO> questions, String templateName) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        Set<Long> seenQuestionIds = new HashSet<>();
        for (TemplateQuestionDTO questionDto : questions) {
            if (questionDto != null && questionDto.isExistingQuestion()) {
                Long questionId = questionDto.getQuestionId();
                if (questionId != null && !seenQuestionIds.add(questionId)) {
                    throw new InvalidInputException(
                            "A mesma questão não pode ser adicionada mais de uma vez no template '"
                            + templateName + "'. ID duplicado: " + questionId);
                }
            }
        }
    }

    private QuestionEntity createNewQuestion(TemplateQuestionDTO questionDto, Long clientId) {
        if (questionDto.getQuestionText() == null || questionDto.getQuestionText().trim().isEmpty()) {
            throw new InvalidInputException("Texto da questão é obrigatório!");
        }

        if (questionDto.getType() == null) {
            throw new InvalidInputException("Tipo da questão é obrigatório!");
        }

        if (TypeQuestionEnum.CHECKBOX.equals(questionDto.getType())) {
            if (questionDto.getOptionIds() == null || questionDto.getOptionIds().isEmpty()) {
                throw new InvalidInputException(
                        "Questões do tipo CHECKBOX devem ter pelo menos uma opção associada!");
            }
        }

        QuestionEntity newQuestion = new QuestionEntity();
        newQuestion.setQuestionText(questionDto.getQuestionText());
        newQuestion.setType(questionDto.getType());

        ClientEntity client = new ClientEntity();
        client.setId(clientId);
        newQuestion.setClient(client);

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

        return questionService.save(newQuestion);
    }

    private void validateTemplatesBelongToDam(Set<TemplateQuestionnaireEntity> templates, Long damId, String damName) {
        if (templates == null || templates.isEmpty()) {
            return;
        }

        for (TemplateQuestionnaireEntity template : templates) {
            if (template.getDam() == null || template.getDam().getId() == null) {
                throw new BusinessRuleException(
                        "O template '" + template.getName() + "' não possui uma barragem associada."
                );
            }

            Long templateDamId = template.getDam().getId();
            if (!templateDamId.equals(damId)) {
                TemplateQuestionnaireEntity fullTemplate = templateQuestionnaireRepository.findById(template.getId())
                        .orElse(template);

                String templateDamName = fullTemplate.getDam() != null
                        ? damService.findById(fullTemplate.getDam().getId()).getName() : "desconhecida";

                throw new BusinessRuleException(
                        "Não é possível adicionar o template '" + template.getName()
                        + "' ao checklist. O template pertence à barragem '" + templateDamName
                        + "', mas o checklist está vinculado à barragem '" + damName + "'. "
                        + "Todos os templates de um checklist devem pertencer à mesma barragem."
                );
            }
        }
    }

    public List<ChecklistCompleteDTO> findByDamIdDTO(Long damId) {
        ChecklistEntity checklist = checklistRepository.findByDamIdWithFullDetails(damId);
        if (checklist == null) {
            return List.of();
        }
        return List.of(convertToCompleteDTO(checklist));
    }

    public ChecklistCompleteDTO findChecklistForDamDTO(Long damId, Long checklistId) {
        ChecklistEntity checklist = findById(checklistId);

        if (checklist.getDam() != null && checklist.getDam().getId().equals(damId)) {
            return convertToCompleteDTO(checklist);
        } else {
            throw new NotFoundException("O checklist não pertence à barragem especificada!");
        }
    }

    public ChecklistEntity findChecklistForDam(Long damId, Long checklistId) {
        ChecklistEntity checklist = findById(checklistId);

        if (checklist.getDam() != null && checklist.getDam().getId().equals(damId)) {
            return checklist;
        } else {
            throw new NotFoundException("O checklist não pertence à barragem especificada!");
        }
    }

    private ChecklistCompleteDTO convertToCompleteDTO(ChecklistEntity entity) {
        ChecklistCompleteDTO dto = new ChecklistCompleteDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCreatedAt(entity.getCreatedAt());

        Set<Long> seenCtIds = new HashSet<>();
        List<ChecklistCompleteDTO.TemplateQuestionnaireDTO> templateDTOs = entity.getChecklistTemplates().stream()
                .filter(ct -> seenCtIds.add(ct.getId()))
                .map(ct -> {
                    TemplateQuestionnaireEntity template = ct.getTemplateQuestionnaire();
                    ChecklistCompleteDTO.TemplateQuestionnaireDTO templateDTO
                            = new ChecklistCompleteDTO.TemplateQuestionnaireDTO();
                    templateDTO.setId(template.getId());
                    templateDTO.setName(template.getName());
                    templateDTO.setOrderIndex(ct.getOrderIndex());

                    Set<ChecklistCompleteDTO.TemplateQuestionnaireQuestionDTO> questionDTOs = template.getTemplateQuestions().stream()
                            .map(tqq -> {
                                ChecklistCompleteDTO.TemplateQuestionnaireQuestionDTO tqqDTO
                                        = new ChecklistCompleteDTO.TemplateQuestionnaireQuestionDTO();
                                tqqDTO.setId(tqq.getId());
                                tqqDTO.setOrderIndex(tqq.getOrderIndex());

                                ChecklistCompleteDTO.QuestionDTO questionDTO = new ChecklistCompleteDTO.QuestionDTO();
                                questionDTO.setId(tqq.getQuestion().getId());
                                questionDTO.setQuestionText(tqq.getQuestion().getQuestionText());
                                questionDTO.setType(tqq.getQuestion().getType().toString());

                                Set<ChecklistCompleteDTO.OptionDTO> optionDTOs = tqq.getQuestion().getOptions().stream()
                                        .map(opt -> new ChecklistCompleteDTO.OptionDTO(
                                        opt.getId(),
                                        opt.getLabel(),
                                        opt.getValue(),
                                        opt.getOrderIndex()))
                                        .collect(Collectors.toSet());
                                questionDTO.setOptions(optionDTOs);

                                tqqDTO.setQuestion(questionDTO);
                                return tqqDTO;
                            })
                            .collect(Collectors.toSet());
                    templateDTO.setTemplateQuestions(questionDTOs);

                    return templateDTO;
                })
                .collect(Collectors.toList());
        dto.setTemplateQuestionnaires(templateDTOs);

        if (entity.getDam() != null) {
            DamEntity dam = entity.getDam();
            ChecklistCompleteDTO.DamDTO damDTO = new ChecklistCompleteDTO.DamDTO();
            damDTO.setId(dam.getId());
            damDTO.setName(dam.getName());
            damDTO.setLatitude(dam.getLatitude());
            damDTO.setLongitude(dam.getLongitude());

            if (dam.getClient() != null) {
                ChecklistCompleteDTO.ClientDTO clientDTO = new ChecklistCompleteDTO.ClientDTO();
                clientDTO.setId(dam.getClient().getId());
                clientDTO.setName(dam.getClient().getName());
                damDTO.setClient(clientDTO);
            } else {
                damDTO.setClient(null);
            }

            dto.setDam(damDTO);
        }

        return dto;
    }

    @Transactional
    public ChecklistTemplateAssociationResponseDTO updateTemplateAssociation(
            Long checklistId,
            ChecklistTemplateAssociationDTO dto) {

        String actor = resolveActor();
        logChecklistAudit(
                "CHECKLIST_TEMPLATE_ASSOCIATION",
                "START",
                actor,
                "checklistId=" + checklistId + " templateId=" + dto.getTemplateId()
                + " action=" + dto.getAction(),
                null
        );

        try {
            if (dto.getAction() == null) {
                throw new InvalidInputException("Acao e obrigatoria!");
            }

            ChecklistEntity checklist = checklistRepository.findByIdWithTemplates(checklistId)
                    .orElseThrow(() -> new NotFoundException("Checklist nao encontrada para id: " + checklistId));

            TemplateQuestionnaireEntity template = templateQuestionnaireRepository.findById(dto.getTemplateId())
                    .orElseThrow(() -> new NotFoundException("Template nao encontrado com ID: " + dto.getTemplateId()));

            if (!template.getDam().getId().equals(checklist.getDam().getId())) {
                throw new BusinessRuleException(
                        "Nao e possivel associar templates de outra barragem ao checklist."
                );
            }

            boolean alreadyAssociated = checklistTemplateRepository
                    .findByChecklistIdAndTemplateQuestionnaireId(checklistId, dto.getTemplateId())
                    .isPresent();

            Integer resultOrderIndex = null;

            if (dto.getAction() == AssociationAction.ASSOCIATE) {
                if (!alreadyAssociated) {
                    long count = checklistTemplateRepository.countByChecklistId(checklistId);
                    int newIndex = dto.getOrderIndex() != null ? dto.getOrderIndex() : (int) count + 1;

                    if (dto.getOrderIndex() != null && dto.getOrderIndex() <= count) {
                        List<ChecklistTemplateEntity> existing = checklistTemplateRepository
                                .findByChecklistIdOrderByOrderIndex(checklistId);
                        for (ChecklistTemplateEntity e : existing) {
                            if (e.getOrderIndex() >= newIndex) {
                                checklistTemplateRepository.updateOrderIndex(e.getId(), e.getOrderIndex() + 1);
                            }
                        }
                    }

                    ChecklistTemplateEntity ct = new ChecklistTemplateEntity();
                    ct.setChecklist(checklist);
                    ct.setTemplateQuestionnaire(template);
                    ct.setOrderIndex(newIndex);
                    checklistTemplateRepository.save(ct);
                    resultOrderIndex = newIndex;
                }
            } else {
                ChecklistTemplateEntity ct = checklistTemplateRepository
                        .findByChecklistIdAndTemplateQuestionnaireId(checklistId, dto.getTemplateId())
                        .orElseThrow(() -> new NotFoundException("Template nao esta associado a este checklist."));

                int removedIndex = ct.getOrderIndex();
                checklistTemplateRepository.delete(ct);

                List<ChecklistTemplateEntity> remaining = checklistTemplateRepository
                        .findByChecklistIdOrderByOrderIndex(checklistId);
                for (ChecklistTemplateEntity e : remaining) {
                    if (e.getOrderIndex() > removedIndex) {
                        checklistTemplateRepository.updateOrderIndex(e.getId(), e.getOrderIndex() - 1);
                    }
                }
            }

            long templateCount = checklistTemplateRepository.countByChecklistId(checklistId);

            ChecklistTemplateAssociationResponseDTO result = new ChecklistTemplateAssociationResponseDTO();
            result.setChecklistId(checklistId);
            result.setTemplateId(dto.getTemplateId());
            result.setAction(dto.getAction());
            result.setTemplateCount((int) templateCount);
            result.setOrderIndex(resultOrderIndex);

            logChecklistAudit(
                    "CHECKLIST_TEMPLATE_ASSOCIATION",
                    "SUCCESS",
                    actor,
                    "checklistId=" + checklistId + " templateId=" + dto.getTemplateId()
                    + " action=" + dto.getAction() + " templateCount=" + result.getTemplateCount(),
                    null
            );

            return result;
        } catch (RuntimeException e) {
            logChecklistAudit(
                    "CHECKLIST_TEMPLATE_ASSOCIATION",
                    "ERROR",
                    actor,
                    "checklistId=" + checklistId + " templateId=" + dto.getTemplateId()
                    + " action=" + dto.getAction(),
                    e
            );
            throw e;
        }
    }

    @Transactional
    public List<ChecklistTemplateAssociationResponseDTO> reorderTemplates(Long checklistId, TemplateReorderDTO dto) {
        checklistRepository.findByIdWithDam(checklistId)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrado para id: " + checklistId));

        List<ChecklistTemplateEntity> existing = checklistTemplateRepository
                .findByChecklistIdOrderByOrderIndex(checklistId);

        if (existing.size() != dto.getTemplates().size()) {
            throw new InvalidInputException(
                    "Número de templates na reordenação (" + dto.getTemplates().size()
                    + ") não corresponde ao número de templates no checklist (" + existing.size() + ").");
        }

        Set<Long> existingIds = existing.stream()
                .map(ChecklistTemplateEntity::getId)
                .collect(Collectors.toSet());
        for (TemplateOrderDTO orderDto : dto.getTemplates()) {
            if (!existingIds.contains(orderDto.getChecklistTemplateId())) {
                throw new InvalidInputException(
                        "ChecklistTemplate ID " + orderDto.getChecklistTemplateId() + " não pertence a este checklist.");
            }
        }

        Set<Integer> seenIndexes = new HashSet<>();
        for (TemplateOrderDTO orderDto : dto.getTemplates()) {
            if (!seenIndexes.add(orderDto.getOrderIndex())) {
                throw new InvalidInputException("Índice de ordem duplicado: " + orderDto.getOrderIndex());
            }
        }

        int n = dto.getTemplates().size();
        for (TemplateOrderDTO orderDto : dto.getTemplates()) {
            if (orderDto.getOrderIndex() < 1 || orderDto.getOrderIndex() > n) {
                throw new InvalidInputException(
                        "Índices de ordem devem ser uma sequência contínua de 1 a " + n + ".");
            }
        }

        Map<Long, ChecklistTemplateEntity> ctById = existing.stream()
                .collect(Collectors.toMap(ChecklistTemplateEntity::getId, ct -> ct));

        List<ChecklistTemplateAssociationResponseDTO> results = new ArrayList<>();
        for (TemplateOrderDTO orderDto : dto.getTemplates()) {
            checklistTemplateRepository.updateOrderIndex(orderDto.getChecklistTemplateId(), orderDto.getOrderIndex());
            ChecklistTemplateEntity ct = ctById.get(orderDto.getChecklistTemplateId());

            ChecklistTemplateAssociationResponseDTO resp = new ChecklistTemplateAssociationResponseDTO();
            resp.setChecklistId(checklistId);
            resp.setTemplateId(ct.getTemplateQuestionnaire().getId());
            resp.setTemplateCount(n);
            resp.setOrderIndex(orderDto.getOrderIndex());
            results.add(resp);
        }

        return results;
    }

    @Transactional
    public ChecklistEntity replicateChecklist(Long sourceChecklistId, Long targetDamId) {
        String actor = resolveActor();
        logChecklistAudit(
                "CHECKLIST_REPLICATE",
                "START",
                actor,
                "sourceChecklistId=" + sourceChecklistId + " targetDamId=" + targetDamId,
                null
        );

        log.info("Iniciando replicação do checklist {} para a barragem {}", sourceChecklistId, targetDamId);

        try {

            ChecklistEntity sourceChecklist = findById(sourceChecklistId);

            DamEntity targetDam = damService.findById(targetDamId);

            DamEntity sourceDam = sourceChecklist.getDam();
            if (!sourceDam.getClient().getId().equals(targetDam.getClient().getId())) {
                throw new BusinessRuleException(
                        "Não é possível replicar checklist entre barragens de clientes diferentes. "
                        + "Barragem de origem pertence ao cliente '" + sourceDam.getClient().getName()
                        + "' e barragem de destino pertence ao cliente '" + targetDam.getClient().getName() + "'.");
            }

            ChecklistEntity existingChecklist = checklistRepository.findByDamId(targetDamId);
            if (existingChecklist != null) {
                throw new BusinessRuleException(
                        "A barragem de destino '" + targetDam.getName() + "' já possui um checklist cadastrado. "
                        + "Não é possível criar outro checklist para a mesma barragem.");
            }

            String replicatedChecklistName = defaultReplicatedChecklistName(targetDam);

            if (checklistRepository.existsByNameAndDamId(replicatedChecklistName, targetDamId)) {
                throw new DuplicateResourceException(
                        "Já existe um checklist com o nome '" + replicatedChecklistName
                        + "' para a barragem de destino.");
            }

            log.info("Validações concluídas. Iniciando criação de cópias...");

            ChecklistEntity newChecklist = new ChecklistEntity();
            newChecklist.setName(replicatedChecklistName);
            newChecklist.setDam(targetDam);

            newChecklist = checklistRepository.save(newChecklist);
            log.info("Checklist replicado criado com ID: {}", newChecklist.getId());

            int templateCount = 0;
            Set<Long> seenSourceCtIds = new HashSet<>();
            for (ChecklistTemplateEntity sourceCt : sourceChecklist.getChecklistTemplates()) {
                if (!seenSourceCtIds.add(sourceCt.getId())) continue;
                TemplateQuestionnaireEntity sourceTemplate = sourceCt.getTemplateQuestionnaire();

                TemplateQuestionnaireEntity newTemplate = new TemplateQuestionnaireEntity();
                newTemplate.setName(sourceTemplate.getName());
                newTemplate.setDam(targetDam);
                newTemplate.setTemplateQuestions(new HashSet<>());

                newTemplate = templateQuestionnaireRepository.save(newTemplate);
                log.debug("Template replicado: {} com ID {}", newTemplate.getName(), newTemplate.getId());

                List<TemplateQuestionnaireQuestionEntity> sortedQuestions = sourceTemplate.getTemplateQuestions()
                        .stream()
                        .sorted(java.util.Comparator.comparing(TemplateQuestionnaireQuestionEntity::getOrderIndex))
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

                ChecklistTemplateEntity newCt = new ChecklistTemplateEntity();
                newCt.setChecklist(newChecklist);
                newCt.setTemplateQuestionnaire(newTemplate);
                newCt.setOrderIndex(sourceCt.getOrderIndex());
                newChecklist.getChecklistTemplates().add(newCt);

                log.debug("Template '{}' replicado reutilizando {} questões", newTemplate.getName(), questionCount);
                templateCount++;
            }

            ChecklistEntity saved = checklistRepository.save(newChecklist);

            log.info("Replicação concluída: Checklist {} criado com {} template(s) para barragem {}",
                    saved.getId(), templateCount, targetDamId);

            ChecklistEntity result = findById(saved.getId());

            logChecklistAudit(
                    "CHECKLIST_REPLICATE",
                    "SUCCESS",
                    actor,
                    "newChecklistId=" + result.getId() + " targetDamId=" + targetDamId
                    + " templateCount=" + result.getChecklistTemplates().size(),
                    null
            );

            return result;
        } catch (RuntimeException e) {
            logChecklistAudit(
                    "CHECKLIST_REPLICATE",
                    "ERROR",
                    actor,
                    "sourceChecklistId=" + sourceChecklistId + " targetDamId=" + targetDamId,
                    e
            );
            throw e;
        }
    }

    private String resolveActor() {
        try {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            String email = user.getEmail() != null ? user.getEmail() : "";
            return user.getName() + " (ID: " + user.getId() + ", Email: " + email + ")";
        } catch (Exception e) {
            return "Anonimo/Não autenticado";
        }
    }

    private String defaultReplicatedChecklistName(DamEntity targetDam) {
        return "Checklist " + targetDam.getName();
    }

    private void logChecklistAudit(String action, String status, String actor, String details, Exception error) {
        String safeDetails = details != null ? details : "";
        String message = "CHECKLIST_AUDIT action=" + action
                + " status=" + status
                + " actor=" + actor
                + " " + safeDetails;

        if (error == null) {
            log.info(message);
        } else {
            log.error(message + " error=" + error.getMessage(), error);
        }
    }

    private String templateIdsSummary(List<ChecklistTemplateEntity> templates) {
        if (templates == null) {
            return "templates=null";
        }
        if (templates.isEmpty()) {
            return "templates=[]";
        }
        List<String> ids = templates.stream()
                .map(ct -> ct.getTemplateQuestionnaire() != null && ct.getTemplateQuestionnaire().getId() != null
                ? ct.getTemplateQuestionnaire().getId().toString() : "null")
                .collect(Collectors.toList());
        return "templateIds=" + ids;
    }

    private String templateDtoSummary(List<TemplateInChecklistDTO> templates) {
        if (templates == null) {
            return "templates=null";
        }
        if (templates.isEmpty()) {
            return "templates=[]";
        }

        int existingCount = 0;
        int newCount = 0;
        List<Long> existingIds = new ArrayList<>();

        for (TemplateInChecklistDTO templateDto : templates) {
            if (templateDto == null) {
                continue;
            }
            if (templateDto.isExistingTemplate()) {
                existingCount++;
                existingIds.add(templateDto.getTemplateId());
            } else if (templateDto.isNewTemplate()) {
                newCount++;
            }
        }

        return "templates=existing:" + existingCount
                + " new:" + newCount
                + " existingIds=" + existingIds;
    }
}
