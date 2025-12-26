package com.geosegbar.infra.template_questionnaire.services;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist.services.ChecklistService;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionDTO;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionnaireCreationDTO;
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
    private final OptionRepository optionRepository;
    private final DamRepository damRepository;
    private final ChecklistRepository checklistRepository;
    private final CacheManager checklistCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * ⭐ NOVO: Invalida TODOS os caches de checklist
     */
    private void evictAllChecklistCaches() {
        log.info("Invalidando TODOS os caches de checklist devido a mudança em TemplateQuestionnaire");

        var checklistByIdCache = checklistCacheManager.getCache("checklistById");
        if (checklistByIdCache != null) {
            checklistByIdCache.clear();
        }

        var checklistsByDamCache = checklistCacheManager.getCache("checklistsByDam");
        if (checklistsByDamCache != null) {
            checklistsByDamCache.clear();
        }

        var checklistsWithAnswersByDamCache = checklistCacheManager.getCache("checklistsWithAnswersByDam");
        if (checklistsWithAnswersByDamCache != null) {
            checklistsWithAnswersByDamCache.clear();
        }

        var checklistsWithAnswersByClientCache = checklistCacheManager.getCache("checklistsWithAnswersByClient");
        if (checklistsWithAnswersByClientCache != null) {
            checklistsWithAnswersByClientCache.clear();
        }

        evictCachesByPattern("checklistForDam", "*");
    }

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

    @Transactional
    public void deleteById(Long id) {
        templateQuestionnaireRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Template não encontrado para exclusão!"));
        templateQuestionnaireRepository.deleteById(id);

        evictAllChecklistCaches();
        log.info("Template {} deletado. Caches de checklist invalidados.", id);
    }

    @Transactional
    public TemplateQuestionnaireEntity save(TemplateQuestionnaireEntity template) {
        TemplateQuestionnaireEntity saved = templateQuestionnaireRepository.save(template);

        evictAllChecklistCaches();
        log.info("Template {} criado. Caches de checklist invalidados.", saved.getId());

        return saved;
    }

    @Transactional
    public TemplateQuestionnaireEntity update(TemplateQuestionnaireEntity template) {
        templateQuestionnaireRepository.findById(template.getId())
                .orElseThrow(() -> new NotFoundException("Template não encontrado para atualização!"));
        TemplateQuestionnaireEntity saved = templateQuestionnaireRepository.save(template);

        evictAllChecklistCaches();
        log.info("Template {} atualizado. Caches de checklist invalidados.", template.getId());

        return saved;
    }

    @Transactional
    public TemplateQuestionnaireEntity createWithQuestions(TemplateQuestionnaireCreationDTO dto) {
        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName(dto.getName());
        template.setTemplateQuestions(new HashSet<>());

        template = templateQuestionnaireRepository.save(template);

        for (TemplateQuestionDTO questionDto : dto.getQuestions()) {
            QuestionEntity question = questionRepository.findById(questionDto.getQuestionId())
                    .orElseThrow(() -> new NotFoundException(
                    "Questão não encontrada com ID: " + questionDto.getQuestionId()));

            TemplateQuestionnaireQuestionEntity templateQuestion = new TemplateQuestionnaireQuestionEntity();
            templateQuestion.setTemplateQuestionnaire(template);
            templateQuestion.setQuestion(question);
            templateQuestion.setOrderIndex(questionDto.getOrderIndex());

            template.getTemplateQuestions().add(templateQuestion);
        }

        TemplateQuestionnaireEntity saved = templateQuestionnaireRepository.save(template);

        evictAllChecklistCaches();
        log.info("Template {} criado com questões. Caches de checklist invalidados.", saved.getId());

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

    /**
     * Replica um template de questionário completo de uma barragem para outra.
     * Cria cópias independentes de todas as questões e opções.
     *
     * @param sourceTemplateId ID do template de origem
     * @param targetDamId ID da barragem de destino
     * @return Template replicado com todas as questões e opções
     */
    @Transactional
    public TemplateQuestionnaireEntity replicateTemplate(Long sourceTemplateId, Long targetDamId) {
        log.info("Iniciando replicação do template {} para a barragem {}", sourceTemplateId, targetDamId);

        // 1. Validar template de origem
        TemplateQuestionnaireEntity sourceTemplate = templateQuestionnaireRepository
                .findByIdWithFullDetails(sourceTemplateId)
                .orElseThrow(() -> new NotFoundException(
                "Template de origem não encontrado com ID: " + sourceTemplateId));

        // 2. Validar barragem de destino
        damRepository.findById(targetDamId)
                .orElseThrow(() -> new NotFoundException(
                "Barragem de destino não encontrada com ID: " + targetDamId));

        // 3. Buscar checklist da barragem de destino
        ChecklistEntity targetChecklist = checklistRepository.findByDamId(targetDamId);
        if (targetChecklist == null) {
            throw new NotFoundException(
                    "Não existe checklist para a barragem de destino. Crie um checklist primeiro.");
        }

        // 4. Validar se já existe template com o mesmo nome nessa barragem
        String templateName = sourceTemplate.getName();
        boolean templateExists = targetChecklist.getTemplateQuestionnaires().stream()
                .anyMatch(t -> t.getName().equalsIgnoreCase(templateName));

        if (templateExists) {
            throw new DuplicateResourceException(
                    "Já existe um template com o nome '" + templateName
                    + "' na barragem de destino. Escolha outro template ou renomeie o existente.");
        }

        log.info("Validações concluídas. Iniciando criação de cópias...");

        // 5. Criar novo template
        TemplateQuestionnaireEntity newTemplate = new TemplateQuestionnaireEntity();
        newTemplate.setName(sourceTemplate.getName());
        newTemplate.setTemplateQuestions(new HashSet<>());
        newTemplate.setChecklists(new HashSet<>());
        newTemplate.getChecklists().add(targetChecklist);

        newTemplate = templateQuestionnaireRepository.save(newTemplate);
        log.info("Template replicado criado com ID: {}", newTemplate.getId());

        // 6. Processar cada questão do template original
        List<TemplateQuestionnaireQuestionEntity> sortedQuestions = sourceTemplate.getTemplateQuestions()
                .stream()
                .sorted(Comparator.comparing(TemplateQuestionnaireQuestionEntity::getOrderIndex))
                .collect(Collectors.toList());

        int questionCount = 0;
        for (TemplateQuestionnaireQuestionEntity sourceTemplateQuestion : sortedQuestions) {
            QuestionEntity sourceQuestion = sourceTemplateQuestion.getQuestion();

            // 6.1. Criar cópia da questão
            QuestionEntity newQuestion = new QuestionEntity();
            newQuestion.setQuestionText(sourceQuestion.getQuestionText());
            newQuestion.setType(sourceQuestion.getType());
            newQuestion.setOptions(new HashSet<>());

            // 6.2. Replicar opções da questão (criar cópias independentes)
            for (OptionEntity sourceOption : sourceQuestion.getOptions()) {
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
            log.debug("Questão replicada: {} com {} opções",
                    newQuestion.getQuestionText(), newQuestion.getOptions().size());

            // 6.3. Criar TemplateQuestionnaireQuestion
            TemplateQuestionnaireQuestionEntity newTemplateQuestion = new TemplateQuestionnaireQuestionEntity();
            newTemplateQuestion.setTemplateQuestionnaire(newTemplate);
            newTemplateQuestion.setQuestion(newQuestion);
            newTemplateQuestion.setOrderIndex(sourceTemplateQuestion.getOrderIndex());

            newTemplate.getTemplateQuestions().add(newTemplateQuestion);
            questionCount++;
        }

        // 7. Salvar template com todas as questões
        newTemplate = templateQuestionnaireRepository.save(newTemplate);

        log.info("Replicação concluída: Template {} criado com {} questões para barragem {}",
                newTemplate.getId(), questionCount, targetDamId);

        // 8. Invalidar caches
        evictAllChecklistCaches();
        log.info("Caches de checklist invalidados após replicação");

        return newTemplate;
    }
}
