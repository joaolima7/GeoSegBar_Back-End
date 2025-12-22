package com.geosegbar.infra.answer.services;

import java.util.List;
import java.util.Set;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final CacheManager checklistCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * ⭐ NOVO: Invalida TODOS os caches de checklistResponse
     */
    private void evictAllChecklistResponseCaches() {
        log.info("Invalidando TODOS os caches de checklistResponse devido a mudança em Answer");

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
        }
    }

    @Transactional
    public void deleteById(Long id) {
        answerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resposta não encontrada para exclusão!"));
        answerRepository.deleteById(id);

        evictAllChecklistResponseCaches();
        log.info("Answer {} deletado. Caches de checklistResponse invalidados.", id);
    }

    @Transactional
    public AnswerEntity save(AnswerEntity answer) {
        validateAnswerByType(answer);
        AnswerEntity saved = answerRepository.save(answer);

        evictAllChecklistResponseCaches();
        log.info("Answer {} criado. Caches de checklistResponse invalidados.", saved.getId());

        return saved;
    }

    @Transactional
    public AnswerEntity update(AnswerEntity answer) {
        answerRepository.findById(answer.getId())
                .orElseThrow(() -> new NotFoundException("Resposta não encontrada para atualização!"));
        validateAnswerByType(answer);
        AnswerEntity saved = answerRepository.save(answer);

        evictAllChecklistResponseCaches();
        log.info("Answer {} atualizado. Caches de checklistResponse invalidados.", answer.getId());

        return saved;
    }

    public AnswerEntity findById(Long id) {
        return answerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resposta não encontrada!"));
    }

    public List<AnswerEntity> findAll() {
        return answerRepository.findAll();
    }

    private void validateAnswerByType(AnswerEntity answer) {
        QuestionEntity question = answer.getQuestion();

        if (question == null) {
            throw new InvalidInputException("A resposta deve estar associada a uma pergunta");
        }

        if (TypeQuestionEnum.TEXT.equals(question.getType())) {
            if (answer.getComment() == null || answer.getComment().trim().isEmpty()) {
                throw new InvalidInputException("Respostas para perguntas do tipo TEXTO devem ter o campo de texto preenchido!");
            }

            if (answer.getSelectedOptions() != null && !answer.getSelectedOptions().isEmpty()) {
                throw new InvalidInputException("Respostas para perguntas do tipo TEXTO não devem ter opções selecionadas!");
            }
        } else if (TypeQuestionEnum.CHECKBOX.equals(question.getType())) {
            if (answer.getSelectedOptions() == null || answer.getSelectedOptions().isEmpty()) {
                throw new InvalidInputException("Respostas para perguntas do tipo CHECKBOX devem ter pelo menos uma opção selecionada!");
            }
        }
    }
}
