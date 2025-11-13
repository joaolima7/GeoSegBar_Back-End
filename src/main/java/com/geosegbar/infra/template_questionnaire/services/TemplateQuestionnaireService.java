package com.geosegbar.infra.template_questionnaire.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist.services.ChecklistService;
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
}
