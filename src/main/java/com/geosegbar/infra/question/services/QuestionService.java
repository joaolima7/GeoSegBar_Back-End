package com.geosegbar.infra.question.services;

import java.util.List;
import java.util.Set;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ClientRepository clientRepository;
    private final CacheManager checklistCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    private void evictAllChecklistCaches() {
        log.info("Invalidando TODOS os caches de checklist devido a mudança em Question");

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
                log.debug("Invalidando {} keys do cache {} com pattern {}",
                        keys.size(), cacheName, pattern);
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Erro ao invalidar cache por pattern: cacheName={}, pattern={}",
                    cacheName, pattern, e);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Questão não encontrada para exclusão!"));
        questionRepository.deleteById(id);

        evictAllChecklistCaches();
        log.info("Questão {} deletada. Caches de checklist invalidados.", id);
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

        evictAllChecklistCaches();
        log.info("Questão {} criada. Caches de checklist invalidados.", saved.getId());

        return saved;
    }

    @Transactional
    public QuestionEntity update(QuestionEntity question) {
        questionRepository.findById(question.getId())
                .orElseThrow(() -> new NotFoundException("Questão não encontrada para atualização!"));

        if (question.getClient() == null || question.getClient().getId() == null) {
            throw new InvalidInputException("Questão deve estar associada a um cliente!");
        }

        if (!clientRepository.existsById(question.getClient().getId())) {
            throw new NotFoundException("Cliente não encontrado com ID: " + question.getClient().getId());
        }

        validateQuestionByType(question);
        QuestionEntity saved = questionRepository.save(question);

        evictAllChecklistCaches();
        log.info("Questão {} atualizada. Caches de checklist invalidados.", question.getId());

        return saved;
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
