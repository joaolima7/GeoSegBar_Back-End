package com.geosegbar.infra.checklist.services;

import java.util.ArrayList;
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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final DamService damService;
    private final AnswerRepository answerRepository;
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

        if (checklist.getDams() == null || checklist.getDams().isEmpty()) {
            throw new InvalidInputException("Checklist deve estar vinculado a uma barragem.");
        }
        if (checklist.getDams().size() > 1) {
            throw new InvalidInputException("Checklist só pode estar vinculado a uma única barragem.");
        }

        DamEntity dam = checklist.getDams().iterator().next();
        Long damId = dam.getId();

        DamEntity fullDam = damService.findById(damId);
        Long clientId = fullDam.getClient().getId();

        // Validação: não permitir criar checklist se a barragem já tiver um
        List<ChecklistEntity> existingChecklists = checklistRepository.findByDams_Id(damId);
        if (!existingChecklists.isEmpty()) {
            throw new BusinessRuleException(
                    "Não é possível criar um novo checklist para esta barragem. "
                    + "A barragem '" + fullDam.getName() + "' já possui um checklist cadastrado. "
                    + "Edite o checklist existente ao invés de criar um novo."
            );
        }

        if (checklistRepository.existsByNameAndDams_Id(checklist.getName(), damId)) {
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

        List<ChecklistEntity> checklists = checklistRepository.findByDamIdWithFullDetails(damId);

        List<ChecklistWithLastAnswersDTO> result = new ArrayList<>();

        for (ChecklistEntity checklist : checklists) {
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
        }

        return result;
    }

    @Transactional()
    @Cacheable(value = "checklistsWithAnswersByClient", key = "#clientId", cacheManager = "checklistCacheManager")
    public List<ChecklistWithLastAnswersAndDamDTO> findAllChecklistsWithLastAnswersByClientId(Long clientId) {

        List<DamEntity> clientDams = damService.findDamsByClientId(clientId);
        List<ChecklistWithLastAnswersAndDamDTO> allChecklists = new ArrayList<>();

        for (DamEntity dam : clientDams) {

            List<ChecklistEntity> damChecklists = checklistRepository.findByDamIdWithFullDetails(dam.getId());

            for (ChecklistEntity checklist : damChecklists) {

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
        }

        allChecklists.sort((a, b) -> {
            int damComparison = a.getDam().getName().compareTo(b.getDam().getName());
            return damComparison != 0 ? damComparison : a.getName().compareTo(b.getName());
        });

        return allChecklists;
    }

    @Transactional

    public ChecklistEntity update(ChecklistEntity checklist) {

        if (checklist.getDams() == null || checklist.getDams().isEmpty()) {
            throw new InvalidInputException("Checklist deve estar vinculado a uma barragem.");
        }
        if (checklist.getDams().size() > 1) {
            throw new InvalidInputException("Checklist só pode estar vinculado a uma única barragem.");
        }

        ChecklistEntity oldChecklist = findById(checklist.getId());
        DamEntity oldDam = oldChecklist.getDams().iterator().next();
        Long oldDamId = oldDam.getId();
        DamEntity oldFullDam = damService.findById(oldDamId);
        Long oldClientId = oldFullDam.getClient().getId();

        DamEntity newDam = checklist.getDams().iterator().next();
        Long newDamId = newDam.getId();
        DamEntity newFullDam = damService.findById(newDamId);
        Long newClientId = newFullDam.getClient().getId();

        if (checklistRepository.existsByNameAndDams_IdAndIdNot(checklist.getName(), newDamId, checklist.getId())) {
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

        DamEntity dam = checklist.getDams().iterator().next();
        Long damId = dam.getId();
        DamEntity fullDam = damService.findById(damId);
        Long clientId = fullDam.getClient().getId();

        checklistRepository.deleteById(id);

        evictChecklistByIdCache(id);

        evictChecklistCachesForDamAndClient(damId, clientId);
    }

    @Cacheable(value = "checklistsByDam", key = "#damId", cacheManager = "checklistCacheManager")
    public List<ChecklistCompleteDTO> findByDamIdDTO(Long damId) {
        List<ChecklistEntity> checklists = checklistRepository.findByDams_Id(damId);
        return convertToCompleteDTOList(checklists);
    }

    @Cacheable(value = "checklistForDam", key = "#damId + '_' + #checklistId", cacheManager = "checklistCacheManager")
    public ChecklistCompleteDTO findChecklistForDamDTO(Long damId, Long checklistId) {
        ChecklistEntity checklist = findById(checklistId);

        if (checklist.getDams().stream().anyMatch(dam -> dam.getId().equals(damId))) {
            return convertToCompleteDTO(checklist);
        } else {
            throw new NotFoundException("O checklist não pertence à barragem especificada!");
        }
    }

    public ChecklistEntity findChecklistForDam(Long damId, Long checklistId) {
        ChecklistEntity checklist = findById(checklistId);

        if (checklist.getDams().stream().anyMatch(dam -> dam.getId().equals(damId))) {
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

        Set<ChecklistCompleteDTO.DamDTO> damDTOs = new HashSet<>();
        for (DamEntity dam : entity.getDams()) {
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

            damDTOs.add(damDTO);
        }
        dto.setDams(damDTOs);

        return dto;
    }

    private List<ChecklistCompleteDTO> convertToCompleteDTOList(List<ChecklistEntity> entities) {
        return entities.stream()
                .map(this::convertToCompleteDTO)
                .collect(Collectors.toList());
    }

}
