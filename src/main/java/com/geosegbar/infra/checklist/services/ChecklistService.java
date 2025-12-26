package com.geosegbar.infra.checklist.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.ChecklistEntity;
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
import com.geosegbar.infra.checklist.dtos.ChecklistCompleteDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersAndDamDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersDTO;
import com.geosegbar.infra.checklist.dtos.OptionDTO;
import com.geosegbar.infra.checklist.dtos.QuestionWithLastAnswerDTO;
import com.geosegbar.infra.checklist.dtos.TemplateQuestionnaireWithAnswersDTO;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
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
    private final OptionRepository optionRepository;
    private final CacheManager checklistCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * ⭐ NOVO: Invalida caches usando pattern matching do Redis
     */
    private void evictCachesByPattern(String cacheName, String pattern) {
        try {
            String fullPattern = cacheName + "::" + pattern;
            Set<String> keys = redisTemplate.keys(fullPattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
        }
    }

    /**
     * ⭐ NOVO: Invalida caches granulares de checklist para uma barragem e
     * cliente
     */
    private void evictChecklistCachesForDamAndClient(Long damId, Long clientId) {

        var checklistsByDamCache = checklistCacheManager.getCache("checklistsByDam");
        if (checklistsByDamCache != null) {
            checklistsByDamCache.evict(damId);
        }

        var checklistsWithAnswersCache = checklistCacheManager.getCache("checklistsWithAnswersByDam");
        if (checklistsWithAnswersCache != null) {
            checklistsWithAnswersCache.evict(damId);
        }

        var checklistsWithAnswersClientCache = checklistCacheManager.getCache("checklistsWithAnswersByClient");
        if (checklistsWithAnswersClientCache != null) {
            checklistsWithAnswersClientCache.evict(clientId);
        }

        evictCachesByPattern("checklistForDam", damId + "_*");
    }

    /**
     * ⭐ NOVO: Invalida cache específico de um checklist
     */
    private void evictChecklistByIdCache(Long checklistId) {

        var cache = checklistCacheManager.getCache("checklistById");
        if (cache != null) {
            cache.evict(checklistId);
        }
    }

    public Page<ChecklistEntity> findAllPaged(Pageable pageable) {
        return checklistRepository.findAllWithDams(pageable);
    }

    @Cacheable(value = "checklistById", key = "#id", cacheManager = "checklistCacheManager")
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

        // Validação: não permitir criar checklist se a barragem já tiver um
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

        ChecklistEntity saved = checklistRepository.save(checklist);

        evictChecklistCachesForDamAndClient(damId, clientId);

        return saved;
    }

    @Transactional()
    @Cacheable(value = "checklistsWithAnswersByDam", key = "#damId", cacheManager = "checklistCacheManager")
    public List<ChecklistWithLastAnswersDTO> findChecklistsWithLastAnswersForDam(Long damId) {

        damService.findById(damId);

        ChecklistEntity checklist = checklistRepository.findByDamIdWithFullDetails(damId);

        List<ChecklistWithLastAnswersDTO> result = new ArrayList<>();

        if (checklist == null) {
            return result; // Retorna lista vazia se n\u00e3o houver checklist
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
    @Cacheable(value = "checklistsWithAnswersByClient", key = "#clientId", cacheManager = "checklistCacheManager")
    public List<ChecklistWithLastAnswersAndDamDTO> findAllChecklistsWithLastAnswersByClientId(Long clientId) {

        List<DamEntity> clientDams = damService.findDamsByClientId(clientId);
        List<ChecklistWithLastAnswersAndDamDTO> allChecklists = new ArrayList<>();

        for (DamEntity dam : clientDams) {

            ChecklistEntity checklist = checklistRepository.findByDamIdWithFullDetails(dam.getId());

            if (checklist == null) {
                continue; // Pula dams sem checklist
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
        DamEntity newFullDam = damService.findById(newDamId);
        Long newClientId = newFullDam.getClient().getId();

        if (checklistRepository.existsByNameAndDamIdAndIdNot(checklist.getName(), newDamId, checklist.getId())) {
            throw new DuplicateResourceException("Já existe um checklist com esse nome para esta barragem.");
        }

        ChecklistEntity saved = checklistRepository.save(checklist);

        evictChecklistByIdCache(checklist.getId());

        if (!oldDamId.equals(newDamId)) {
            evictChecklistCachesForDamAndClient(oldDamId, oldClientId);
        }

        evictChecklistCachesForDamAndClient(newDamId, newClientId);

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

        evictChecklistByIdCache(id);

        evictChecklistCachesForDamAndClient(damId, clientId);
    }

    @Cacheable(value = "checklistsByDam", key = "#damId", cacheManager = "checklistCacheManager")
    public List<ChecklistCompleteDTO> findByDamIdDTO(Long damId) {
        ChecklistEntity checklist = checklistRepository.findByDamId(damId);
        if (checklist == null) {
            return List.of();
        }
        return List.of(convertToCompleteDTO(checklist));
    }

    @Cacheable(value = "checklistForDam", key = "#damId + '_' + #checklistId", cacheManager = "checklistCacheManager")
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

        // 1. Validar checklist de origem
        ChecklistEntity sourceChecklist = checklistRepository
                .findByIdWithFullDetails(sourceChecklistId)
                .orElseThrow(() -> new NotFoundException(
                "Checklist de origem não encontrado com ID: " + sourceChecklistId));

        // 2. Validar barragem de destino
        DamEntity targetDam = damService.findById(targetDamId);

        // 3. Validar se já existe checklist para a barragem de destino
        ChecklistEntity existingChecklist = checklistRepository.findByDamId(targetDamId);
        if (existingChecklist != null) {
            throw new BusinessRuleException(
                    "A barragem de destino '" + targetDam.getName() + "' já possui um checklist cadastrado. "
                    + "Não é possível criar outro checklist para a mesma barragem.");
        }

        // 4. Validar se já existe checklist com o mesmo nome nessa barragem
        if (checklistRepository.existsByNameAndDamId(sourceChecklist.getName(), targetDamId)) {
            throw new DuplicateResourceException(
                    "Já existe um checklist com o nome '" + sourceChecklist.getName()
                    + "' para a barragem de destino.");
        }

        log.info("Validações concluídas. Iniciando criação de cópias...");

        // 5. Criar novo checklist
        ChecklistEntity newChecklist = new ChecklistEntity();
        newChecklist.setName(sourceChecklist.getName());
        newChecklist.setDam(targetDam);
        newChecklist.setTemplateQuestionnaires(new HashSet<>());

        newChecklist = checklistRepository.save(newChecklist);
        log.info("Checklist replicado criado com ID: {}", newChecklist.getId());

        // 6. Replicar cada template do checklist original
        List<TemplateQuestionnaireEntity> sortedTemplates = sourceChecklist.getTemplateQuestionnaires()
                .stream()
                .sorted(Comparator.comparing(TemplateQuestionnaireEntity::getId))
                .collect(Collectors.toList());

        int templateCount = 0;
        for (TemplateQuestionnaireEntity sourceTemplate : sortedTemplates) {

            // 6.1. Criar cópia do template
            TemplateQuestionnaireEntity newTemplate = new TemplateQuestionnaireEntity();
            newTemplate.setName(sourceTemplate.getName());
            newTemplate.setTemplateQuestions(new HashSet<>());
            newTemplate.setChecklists(new HashSet<>());
            newTemplate.getChecklists().add(newChecklist);

            newTemplate = templateQuestionnaireRepository.save(newTemplate);
            log.debug("Template replicado: {} com ID {}", newTemplate.getName(), newTemplate.getId());

            // 6.2. Replicar questões do template
            List<TemplateQuestionnaireQuestionEntity> sortedQuestions = sourceTemplate.getTemplateQuestions()
                    .stream()
                    .sorted(Comparator.comparing(TemplateQuestionnaireQuestionEntity::getOrderIndex))
                    .collect(Collectors.toList());

            int questionCount = 0;
            for (TemplateQuestionnaireQuestionEntity sourceTemplateQuestion : sortedQuestions) {
                QuestionEntity sourceQuestion = sourceTemplateQuestion.getQuestion();

                // 6.2.1. Criar cópia da questão
                QuestionEntity newQuestion = new QuestionEntity();
                newQuestion.setQuestionText(sourceQuestion.getQuestionText());
                newQuestion.setType(sourceQuestion.getType());
                newQuestion.setOptions(new HashSet<>());

                // 6.2.2. Replicar opções da questão
                List<OptionEntity> sortedOptions = sourceQuestion.getOptions()
                        .stream()
                        .sorted(Comparator.comparing(opt -> opt.getOrderIndex() != null ? opt.getOrderIndex() : Integer.valueOf(0)))
                        .collect(Collectors.toList());

                for (OptionEntity sourceOption : sortedOptions) {
                    OptionEntity newOption = new OptionEntity();
                    newOption.setLabel(sourceOption.getLabel());
                    newOption.setValue(sourceOption.getValue());
                    newOption.setOrderIndex(sourceOption.getOrderIndex());
                    newOption.setAnswers(new HashSet<>());
                    newOption.setQuestions(new HashSet<>());

                    newOption = optionRepository.save(newOption);
                    newQuestion.getOptions().add(newOption);
                }

                newQuestion = questionRepository.save(newQuestion);

                // 6.2.3. Criar TemplateQuestionnaireQuestion
                TemplateQuestionnaireQuestionEntity newTemplateQuestion = new TemplateQuestionnaireQuestionEntity();
                newTemplateQuestion.setTemplateQuestionnaire(newTemplate);
                newTemplateQuestion.setQuestion(newQuestion);
                newTemplateQuestion.setOrderIndex(sourceTemplateQuestion.getOrderIndex());

                newTemplate.getTemplateQuestions().add(newTemplateQuestion);
                questionCount++;
            }

            newTemplate = templateQuestionnaireRepository.save(newTemplate);
            newChecklist.getTemplateQuestionnaires().add(newTemplate);

            log.debug("Template '{}' replicado com {} questões", newTemplate.getName(), questionCount);
            templateCount++;
        }

        // 7. Salvar checklist com todos os templates
        newChecklist = checklistRepository.save(newChecklist);

        log.info("Replicação concluída: Checklist {} criado com {} template(s) para barragem {}",
                newChecklist.getId(), templateCount, targetDamId);

        // 8. Invalidar caches
        Long clientId = targetDam.getClient().getId();
        evictChecklistCachesForDamAndClient(targetDamId, clientId);
        log.info("Caches de checklist invalidados após replicação");

        return newChecklist;
    }

}
