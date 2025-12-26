package com.geosegbar.infra.template_questionnaire.services;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist.services.ChecklistService;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
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
    private final DamRepository damRepository;
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

        if (template.getDam() == null || template.getDam().getId() == null) {
            throw new InvalidInputException("Template deve estar vinculado a uma barragem.");
        }

        if (templateQuestionnaireRepository.existsByNameAndDamId(template.getName(), template.getDam().getId())) {
            throw new DuplicateResourceException(
                    "Já existe um template com o nome '" + template.getName() + "' para esta barragem.");
        }

        TemplateQuestionnaireEntity saved = templateQuestionnaireRepository.save(template);

        evictAllChecklistCaches();
        log.info("Template {} criado para barragem {}. Caches de checklist invalidados.",
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

        evictAllChecklistCaches();
        log.info("Template {} atualizado. Caches de checklist invalidados.", template.getId());

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

    public List<TemplateQuestionnaireEntity> findByDamIdOrderedByName(Long damId) {
        if (!damRepository.existsById(damId)) {
            throw new NotFoundException("Barragem não encontrada com ID: " + damId);
        }
        return templateQuestionnaireRepository.findByDamIdOrderByNameAsc(damId);
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

        evictAllChecklistCaches();
        log.info("Caches de checklist invalidados após replicação");

        return newTemplate;
    }
}
