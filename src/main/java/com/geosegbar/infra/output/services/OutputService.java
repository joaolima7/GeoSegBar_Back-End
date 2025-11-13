package com.geosegbar.infra.output.services;

import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.OutputEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.output.persistence.jpa.OutputRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutputService {

    private final OutputRepository outputRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public List<OutputEntity> findByInstrumentId(Long instrumentId) {
        return outputRepository.findByInstrumentIdAndActiveTrue(instrumentId);
    }

    public OutputEntity findById(Long id) {
        return outputRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Output não encontrado com ID: " + id));
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
    @Caching(evict = {
        @CacheEvict(
                value = {"instrumentById", "instrumentWithDetails", "instrumentResponseDTO"},
                key = "#result.instrument.id",
                cacheManager = "instrumentCacheManager"
        ),
        @CacheEvict(
                value = {"instrumentsByDam", "instrumentsByFilters"},
                allEntries = true,
                cacheManager = "instrumentCacheManager"
        ),

        @CacheEvict(
                value = "readingsByOutput",
                key = "#id",
                cacheManager = "readingCacheManager"
        ),
        @CacheEvict(
                value = {"readingsByInstrument", "groupedReadings"},
                key = "#result.instrument.id",
                cacheManager = "readingCacheManager"
        )

    })
    public OutputEntity deleteById(Long id) {
        OutputEntity output = findById(id);
        Long instrumentId = output.getInstrument().getId();
        Long clientId = output.getInstrument().getDam().getClient().getId();

        log.info("Deletando output {} do instrumento {}. Invalidando caches relacionados.",
                id, instrumentId);

        outputRepository.delete(output);

        evictCachesByPattern("instrumentLimitStatus", instrumentId + "_*");
        evictCachesByPattern("clientInstrumentLimitStatuses", clientId + "_*");
        evictCachesByPattern("clientInstrumentLatestGroupedReadings", clientId + "_*");
        evictCachesByPattern("readingsByFilters", instrumentId + "_*");
        evictCachesByPattern("multiInstrumentReadings", "*_" + instrumentId + "_*");
        evictCachesByPattern("multiInstrumentReadings", instrumentId + "_*");
        evictCachesByPattern("multiInstrumentReadings", "*_" + instrumentId);

        return output;
    }
}
