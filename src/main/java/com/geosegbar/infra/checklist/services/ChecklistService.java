package com.geosegbar.infra.checklist.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.ChecklistEntity;
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
import com.geosegbar.infra.checklist.dtos.ChecklistCompleteCreationDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistCompleteDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistCompleteUpdateDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersAndDamDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersDTO;
import com.geosegbar.infra.checklist.dtos.OptionDTO;
import com.geosegbar.infra.checklist.dtos.QuestionWithLastAnswerDTO;
import com.geosegbar.infra.checklist.dtos.TemplateInChecklistDTO;
import com.geosegbar.infra.checklist.dtos.TemplateQuestionnaireWithAnswersDTO;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
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
        ChecklistEntity entity = checklistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrado para id: " + id));
        return convertToCompleteDTO(entity);
    }

    public ChecklistEntity findById(Long id) {
        return checklistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para id: " + id));
    }

    @Transactional
    public ChecklistEntity save(ChecklistEntity checklist) {

        if (checklist.getDam() == null) {
            throw new InvalidInputException("Checklist deve estar vinculado a uma barragem.");
        }

        DamEntity dam = checklist.getDam();
        Long damId = dam.getId();

        DamEntity fullDam = damService.findById(damId);
        Long clientId = fullDam.getClient().getId();

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

        Set<TemplateQuestionnaireEntity> fullTemplates = new HashSet<>();
        if (checklist.getTemplateQuestionnaires() != null && !checklist.getTemplateQuestionnaires().isEmpty()) {
            for (TemplateQuestionnaireEntity template : checklist.getTemplateQuestionnaires()) {
                if (template.getId() == null) {
                    throw new InvalidInputException("ID do template é obrigatório!");
                }

                TemplateQuestionnaireEntity fullTemplate = templateQuestionnaireRepository.findById(template.getId())
                        .orElseThrow(() -> new NotFoundException(
                        "Template não encontrado com ID: " + template.getId()));

                fullTemplates.add(fullTemplate);
            }

            checklist.setTemplateQuestionnaires(fullTemplates);
        }

        validateTemplatesBelongToDam(checklist.getTemplateQuestionnaires(), damId, fullDam.getName());

        ChecklistEntity saved = checklistRepository.save(checklist);

        return saved;
    }

    @Transactional()
    public List<ChecklistWithLastAnswersDTO> findChecklistsWithLastAnswersForDam(Long damId) {

        damService.findById(damId);

        ChecklistEntity checklist = checklistRepository.findByDamIdWithFullDetails(damId);

        List<ChecklistWithLastAnswersDTO> result = new ArrayList<>();

        if (checklist == null) {
            return result;
        }

        ChecklistWithLastAnswersDTO checklistDTO = new ChecklistWithLastAnswersDTO();
        checklistDTO.setId(checklist.getId());
        checklistDTO.setName(checklist.getName());
        checklistDTO.setCreatedAt(checklist.getCreatedAt());

        List<TemplateQuestionnaireWithAnswersDTO> templateDTOs = new ArrayList<>();

        for (TemplateQuestionnaireEntity template : checklist.getTemplateQuestionnaires()) {
            TemplateQuestionnaireWithAnswersDTO templateDTO = new TemplateQuestionnaireWithAnswersDTO();
            templateDTO.setId(template.getId());
            templateDTO.setName(template.getName());

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

        checklistDTO.setTemplateQuestionnaires(templateDTOs);
        result.add(checklistDTO);

        return result;
    }

    @Transactional()
    public List<ChecklistWithLastAnswersAndDamDTO> findAllChecklistsWithLastAnswersByClientId(Long clientId) {

        List<DamEntity> clientDams = damService.findDamsByClientId(clientId);
        List<ChecklistWithLastAnswersAndDamDTO> allChecklists = new ArrayList<>();

        for (DamEntity dam : clientDams) {

            ChecklistEntity checklist = checklistRepository.findByDamIdWithFullDetails(dam.getId());

            if (checklist == null) {
                continue;
            }

            ChecklistWithLastAnswersAndDamDTO checklistDTO = new ChecklistWithLastAnswersAndDamDTO();
            checklistDTO.setId(checklist.getId());
            checklistDTO.setName(checklist.getName());
            checklistDTO.setCreatedAt(checklist.getCreatedAt());

            ChecklistWithLastAnswersAndDamDTO.DamInfoDTO damInfo
                    = new ChecklistWithLastAnswersAndDamDTO.DamInfoDTO(
                            dam.getId(),
                            dam.getName(),
                            dam.getCity(),
                            dam.getState(),
                            dam.getLatitude(),
                            dam.getLongitude()
                    );
            checklistDTO.setDam(damInfo);

            List<TemplateQuestionnaireWithAnswersDTO> templateDTOs = new ArrayList<>();

            for (TemplateQuestionnaireEntity template : checklist.getTemplateQuestionnaires()) {
                TemplateQuestionnaireWithAnswersDTO templateDTO = new TemplateQuestionnaireWithAnswersDTO();
                templateDTO.setId(template.getId());
                templateDTO.setName(template.getName());

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
                            dam.getId(), question.getId(), template.getId());

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

            checklistDTO.setTemplateQuestionnaires(templateDTOs);
            allChecklists.add(checklistDTO);
        }

        allChecklists.sort((a, b) -> {
            int damComparison = a.getDam().getName().compareTo(b.getDam().getName());
            return damComparison != 0 ? damComparison : a.getName().compareTo(b.getName());
        });

        return allChecklists;
    }

    @Transactional
    public ChecklistEntity update(ChecklistEntity checklist) {

        if (checklist.getDam() == null) {
            throw new InvalidInputException("Checklist deve estar vinculado a uma barragem.");
        }

        ChecklistEntity oldChecklist = findById(checklist.getId());
        DamEntity oldDam = oldChecklist.getDam();
        Long oldDamId = oldDam.getId();
        DamEntity oldFullDam = damService.findById(oldDamId);
        Long oldClientId = oldFullDam.getClient().getId();

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

        Set<TemplateQuestionnaireEntity> fullTemplates = new HashSet<>();
        if (checklist.getTemplateQuestionnaires() != null && !checklist.getTemplateQuestionnaires().isEmpty()) {
            for (TemplateQuestionnaireEntity template : checklist.getTemplateQuestionnaires()) {
                if (template.getId() == null) {
                    throw new InvalidInputException("ID do template é obrigatório!");
                }

                TemplateQuestionnaireEntity fullTemplate = templateQuestionnaireRepository.findById(template.getId())
                        .orElseThrow(() -> new NotFoundException(
                        "Template não encontrado com ID: " + template.getId()));

                fullTemplates.add(fullTemplate);
            }

            checklist.setTemplateQuestionnaires(fullTemplates);
        }

        validateTemplatesBelongToDam(checklist.getTemplateQuestionnaires(), newDamId, oldFullDam.getName());

        ChecklistEntity saved = checklistRepository.save(checklist);

        return saved;
    }

    @Transactional

    public void deleteById(Long id) {

        ChecklistEntity checklist = checklistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para exclusão!"));

        DamEntity dam = checklist.getDam();
        Long damId = dam.getId();
        DamEntity fullDam = damService.findById(damId);
        Long clientId = fullDam.getClient().getId();

        checklistRepository.deleteById(id);

    }

    /**
     * Cria um checklist completo com templates e questões (novas ou
     * existentes). Reaproveita a lógica de criação de templates e questões.
     *
     * @param dto Dados do checklist completo
     * @return Checklist criado com todos os templates e questões
     */
    @Transactional
    public ChecklistEntity createComplete(ChecklistCompleteCreationDTO dto) {
        log.info("Iniciando criação de checklist completo: {}", dto.getName());

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
        checklist.setTemplateQuestionnaires(new HashSet<>());

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

            checklist.getTemplateQuestionnaires().add(template);
            templateCount++;
        }

        ChecklistEntity saved = checklistRepository.save(checklist);

        log.info("Checklist completo criado com sucesso: {} com {} template(s)",
                saved.getName(), templateCount);

        return saved;
    }

    /**
     * Atualiza um checklist completo, incluindo templates e questões. Não
     * permite mudança da dam do checklist.
     *
     * @param checklistId ID do checklist a ser atualizado
     * @param dto Dados atualizados do checklist
     * @return Checklist atualizado
     */
    @Transactional
    public ChecklistEntity updateComplete(Long checklistId, ChecklistCompleteUpdateDTO dto) {
        log.info("Iniciando atualização completa do checklist {}", checklistId);

        ChecklistEntity existingChecklist = findById(checklistId);
        DamEntity dam = existingChecklist.getDam();
        Long damId = dam.getId();
        Long clientId = dam.getClient().getId();

        if (checklistRepository.existsByNameAndDamIdAndIdNot(dto.getName(), damId, checklistId)) {
            throw new DuplicateResourceException("Já existe um checklist com esse nome para esta barragem.");
        }

        existingChecklist.setName(dto.getName());

        existingChecklist.getTemplateQuestionnaires().clear();

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

            existingChecklist.getTemplateQuestionnaires().add(template);
            templateCount++;
        }

        ChecklistEntity saved = checklistRepository.save(existingChecklist);

        log.info("Checklist {} atualizado com sucesso com {} template(s)",
                saved.getName(), templateCount);

        return saved;
    }

    /**
     * Cria um novo template com questões (reaproveitando lógica do
     * TemplateQuestionnaireService).
     *
     * @param templateDto DTO com dados do template
     * @param dam Barragem do template
     * @param clientId ID do cliente
     * @return Template criado
     */
    private TemplateQuestionnaireEntity createNewTemplate(
            TemplateInChecklistDTO templateDto, DamEntity dam, Long clientId) {

        if (templateQuestionnaireRepository.existsByNameAndDamId(templateDto.getName(), dam.getId())) {
            throw new DuplicateResourceException(
                    "Já existe um template com o nome '" + templateDto.getName()
                    + "' para a barragem '" + dam.getName() + "'");
        }

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

    /**
     * Cria uma nova questão (reaproveitando lógica do
     * TemplateQuestionnaireService).
     *
     * @param questionDto DTO com dados da questão
     * @param clientId ID do cliente
     * @return Questão criada
     */
    private QuestionEntity createNewQuestion(TemplateQuestionDTO questionDto, Long clientId) {

        if (questionDto.getQuestionText() == null || questionDto.getQuestionText().trim().isEmpty()) {
            throw new InvalidInputException("Texto da questão é obrigatório!");
        }

        if (questionDto.getType() == null) {
            throw new InvalidInputException("Tipo da questão é obrigatório!");
        }

        if (com.geosegbar.common.enums.TypeQuestionEnum.CHECKBOX.equals(questionDto.getType())) {
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

    /**
     * Valida se todos os templates pertencem à mesma barragem do checklist.
     *
     * @param templates Set de templates a serem validados
     * @param damId ID da barragem do checklist
     * @param damName Nome da barragem (para mensagens de erro)
     * @throws BusinessRuleException se algum template não pertencer à barragem
     */
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

        log.info("Validação concluída: todos os {} templates pertencem à barragem {} (ID: {})",
                templates.size(), damName, damId);
    }

    public List<ChecklistCompleteDTO> findByDamIdDTO(Long damId) {
        ChecklistEntity checklist = checklistRepository.findByDamId(damId);
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

        Set<ChecklistCompleteDTO.TemplateQuestionnaireDTO> templateDTOs = entity.getTemplateQuestionnaires().stream()
                .map(template -> {
                    ChecklistCompleteDTO.TemplateQuestionnaireDTO templateDTO
                            = new ChecklistCompleteDTO.TemplateQuestionnaireDTO();
                    templateDTO.setId(template.getId());
                    templateDTO.setName(template.getName());

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
                .collect(Collectors.toSet());
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

    /**
     * Replica um checklist completo de uma barragem para outra. Cria cópias
     * independentes de todos os templates, questões e opções.
     *
     * @param sourceChecklistId ID do checklist de origem
     * @param targetDamId ID da barragem de destino
     * @return Checklist replicado com todos os templates, questões e opções
     */
    @Transactional
    public ChecklistEntity replicateChecklist(Long sourceChecklistId, Long targetDamId) {
        log.info("Iniciando replicação do checklist {} para a barragem {}", sourceChecklistId, targetDamId);

        ChecklistEntity sourceChecklist = checklistRepository
                .findByIdWithFullDetails(sourceChecklistId)
                .orElseThrow(() -> new NotFoundException(
                "Checklist de origem não encontrado com ID: " + sourceChecklistId));

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

        if (checklistRepository.existsByNameAndDamId(sourceChecklist.getName(), targetDamId)) {
            throw new DuplicateResourceException(
                    "Já existe um checklist com o nome '" + sourceChecklist.getName()
                    + "' para a barragem de destino.");
        }

        log.info("Validações concluídas. Iniciando criação de cópias...");

        ChecklistEntity newChecklist = new ChecklistEntity();
        newChecklist.setName(sourceChecklist.getName());
        newChecklist.setDam(targetDam);
        newChecklist.setTemplateQuestionnaires(new HashSet<>());

        newChecklist = checklistRepository.save(newChecklist);
        log.info("Checklist replicado criado com ID: {}", newChecklist.getId());

        List<TemplateQuestionnaireEntity> sortedTemplates = sourceChecklist.getTemplateQuestionnaires()
                .stream()
                .sorted(Comparator.comparing(TemplateQuestionnaireEntity::getId))
                .collect(Collectors.toList());

        int templateCount = 0;
        for (TemplateQuestionnaireEntity sourceTemplate : sortedTemplates) {

            TemplateQuestionnaireEntity newTemplate = new TemplateQuestionnaireEntity();
            newTemplate.setName(sourceTemplate.getName());
            newTemplate.setDam(targetDam);
            newTemplate.setTemplateQuestions(new HashSet<>());
            newTemplate.setChecklists(new HashSet<>());
            newTemplate.getChecklists().add(newChecklist);

            newTemplate = templateQuestionnaireRepository.save(newTemplate);
            log.debug("Template replicado: {} com ID {}", newTemplate.getName(), newTemplate.getId());

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
            newChecklist.getTemplateQuestionnaires().add(newTemplate);

            log.debug("Template '{}' replicado reutilizando {} questões", newTemplate.getName(), questionCount);
            templateCount++;
        }

        newChecklist = checklistRepository.save(newChecklist);

        log.info("Replicação concluída: Checklist {} criado com {} template(s) para barragem {}",
                newChecklist.getId(), templateCount, targetDamId);

        Long clientId = targetDam.getClient().getId();
        log.info("Caches de checklist invalidados após replicação");

        return newChecklist;
    }

}
