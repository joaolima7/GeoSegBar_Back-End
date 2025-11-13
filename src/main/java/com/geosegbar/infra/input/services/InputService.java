package com.geosegbar.infra.input.services;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InputEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.input.persistence.jpa.InputRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InputService {

    private final InputRepository inputRepository;

    public List<InputEntity> findByInstrumentId(Long instrumentId) {
        return inputRepository.findByInstrumentId(instrumentId);
    }

    public InputEntity findById(Long id) {
        return inputRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Input não encontrado com ID: " + id));
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
        )
    })
    public void deleteById(Long id) {
        InputEntity input = findById(id);
        inputRepository.delete(input);
        log.info("Input excluído: {}", input.getName());
    }
}
