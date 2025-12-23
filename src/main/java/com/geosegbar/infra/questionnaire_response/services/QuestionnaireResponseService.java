package com.geosegbar.infra.questionnaire_response.services;

import java.util.List;
import java.util.Set;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireResponseService {

    private final QuestionnaireResponseRepository responseRepository;
    private final CacheManager checklistCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    private void evictAllChecklistResponseCaches() {
        log.info("Invalidando TODOS os caches de checklistResponse devido a mudança em QuestionnaireResponse");

        var checklistResponseByIdCache = checklistCacheManager.getCache("checklistResponseById");
        if (checklistResponseByIdCache != null) {
            checklistResponseByIdCache.clear();
        }

        var checklistResponseDetailCache = checklistCacheManager.getCache("checklistResponseDetail");
        if (checklistResponseDetailCache != null) {
            checklistResponseDetailCache.clear();
        }

        var checklistResponsesByDamCache = checklistCacheManager.getCache("checklistResponsesByDam");
        if (checklistResponsesByDamCache != null) {
            checklistResponsesByDamCache.clear();
        }

        var checklistResponsesByUserCache = checklistCacheManager.getCache("checklistResponsesByUser");
        if (checklistResponsesByUserCache != null) {
            checklistResponsesByUserCache.clear();
        }

        evictCachesByPattern("checklistResponsesByDamPaged", "*");
        evictCachesByPattern("checklistResponsesByUserPaged", "*");
        evictCachesByPattern("checklistResponsesByClient", "*");
        evictCachesByPattern("clientLatestDetailedChecklistResponses", "*");
        evictCachesByPattern("checklistResponsesByDate", "*");
        evictCachesByPattern("checklistResponsesByDatePaged", "*");
        evictCachesByPattern("allChecklistResponsesPaged", "*");

        var checklistsWithAnswersByDamCache = checklistCacheManager.getCache("checklistsWithAnswersByDam");
        if (checklistsWithAnswersByDamCache != null) {
            checklistsWithAnswersByDamCache.clear();
        }

        var checklistsWithAnswersByClientCache = checklistCacheManager.getCache("checklistsWithAnswersByClient");
        if (checklistsWithAnswersByClientCache != null) {
            checklistsWithAnswersByClientCache.clear();
        }
    }

    private void evictCachesByPattern(String cacheName, String pattern) {
        try {
            String fullPattern = cacheName + "::" + pattern;
            Set<String> keys = redisTemplate.keys(fullPattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Erro ao invalidar cache por pattern: {}", cacheName, e);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        responseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resposta do questionário não encontrada para exclusão!"));
        responseRepository.deleteById(id);

        evictAllChecklistResponseCaches();
        log.info("QuestionnaireResponse {} deletado. Caches de checklistResponse invalidados.", id);
    }

    @Transactional
    public QuestionnaireResponseEntity save(QuestionnaireResponseEntity response) {
        QuestionnaireResponseEntity saved = responseRepository.save(response);

        evictAllChecklistResponseCaches();
        log.info("QuestionnaireResponse {} criado. Caches de checklistResponse invalidados.", saved.getId());

        return saved;
    }

    @Transactional
    public QuestionnaireResponseEntity update(QuestionnaireResponseEntity response) {
        responseRepository.findById(response.getId())
                .orElseThrow(() -> new NotFoundException("Resposta do questionário não encontrada para atualização!"));
        QuestionnaireResponseEntity saved = responseRepository.save(response);

        evictAllChecklistResponseCaches();
        log.info("QuestionnaireResponse {} atualizado. Caches de checklistResponse invalidados.", response.getId());

        return saved;
    }

    public QuestionnaireResponseEntity findById(Long id) {
        return responseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resposta do questionário não encontrada!"));
    }

    public List<QuestionnaireResponseEntity> findAll() {
        return responseRepository.findAll();
    }
}
