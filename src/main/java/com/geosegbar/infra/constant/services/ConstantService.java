package com.geosegbar.infra.constant.services;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.constant.persistence.jpa.ConstantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConstantService {

    private final ConstantRepository constantRepository;

    public List<ConstantEntity> findByInstrumentId(Long instrumentId) {
        return constantRepository.findByInstrumentId(instrumentId);
    }

    public ConstantEntity findById(Long id) {
        return constantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Constante não encontrada com ID: " + id));
    }

    public List<Long> findConstantIdsByInstrumentDamId(Long damId) {
        return constantRepository.findConstantIdsByInstrumentDamId(damId);
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
                value = "graphProperties",
                allEntries = true,
                cacheManager = "instrumentGraphCacheManager"
        ),
        @CacheEvict(
                value = {"graphPatternById", "folderWithPatterns", "damFoldersWithPatterns"},
                allEntries = true,
                cacheManager = "instrumentGraphCacheManager"
        )
    })
    public void deleteById(Long id) {
        ConstantEntity constant = findById(id);
        constantRepository.delete(constant);
    }
}
