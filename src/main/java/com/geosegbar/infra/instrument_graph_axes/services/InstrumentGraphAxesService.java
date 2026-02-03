package com.geosegbar.infra.instrument_graph_axes.services;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentGraphAxesEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.instrument_graph_axes.dtos.GraphAxesResponseDTO;
import com.geosegbar.infra.instrument_graph_axes.dtos.UpdateGraphAxesRequestDTO;
import com.geosegbar.infra.instrument_graph_axes.persistence.jpa.InstrumentGraphAxesRepository;
import com.geosegbar.infra.instrument_graph_pattern.services.InstrumentGraphPatternService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstrumentGraphAxesService {

    private final InstrumentGraphAxesRepository axesRepository;
    private final InstrumentGraphPatternService patternService;

    @Transactional
    @Caching(evict = {
        @CacheEvict(
                value = "graphAxes",
                key = "#patternId",
                cacheManager = "instrumentGraphCacheManager"
        ),

        @CacheEvict(
                value = "graphPatternById",
                key = "#patternId",
                cacheManager = "instrumentGraphCacheManager"
        ),

        @CacheEvict(
                value = {"folderWithPatterns", "damFoldersWithPatterns", "graphPatternsByInstrument"},
                allEntries = true,
                cacheManager = "instrumentGraphCacheManager"
        )
    })
    public GraphAxesResponseDTO updateAxes(Long patternId, UpdateGraphAxesRequestDTO req) {

        patternService.findById(patternId);

        InstrumentGraphAxesEntity axes = findByPatternId(patternId);

        axes.setAbscissaPx(req.getAbscissaPx());
        axes.setAbscissaGridLinesEnable(req.getAbscissaGridLinesEnable());
        axes.setPrimaryOrdinatePx(req.getPrimaryOrdinatePx());
        axes.setSecondaryOrdinatePx(req.getSecondaryOrdinatePx());
        axes.setPrimaryOrdinateGridLinesEnable(req.getPrimaryOrdinateGridLinesEnable());

        axes.setPrimaryOrdinateTitle(req.getPrimaryOrdinateTitle());
        axes.setSecondaryOrdinateTitle(req.getSecondaryOrdinateTitle());
        axes.setPrimaryOrdinateSpacing(req.getPrimaryOrdinateSpacing());
        axes.setSecondaryOrdinateSpacing(req.getSecondaryOrdinateSpacing());
        axes.setPrimaryOrdinateInitialValue(req.getPrimaryOrdinateInitialValue());
        axes.setSecondaryOrdinateInitialValue(req.getSecondaryOrdinateInitialValue());
        axes.setPrimaryOrdinateMaximumValue(req.getPrimaryOrdinateMaximumValue());
        axes.setSecondaryOrdinateMaximumValue(req.getSecondaryOrdinateMaximumValue());

        InstrumentGraphAxesEntity saved = axesRepository.save(axes);

        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "graphAxes", key = "#patternId", cacheManager = "instrumentGraphCacheManager")
    public InstrumentGraphAxesEntity findByPatternId(Long patternId) {

        return axesRepository.findByPatternId(patternId)
                .orElseThrow(() -> new NotFoundException("Eixos não encontrados para o padrão ID: " + patternId));
    }

    public GraphAxesResponseDTO mapToResponseDTO(InstrumentGraphAxesEntity axes) {
        return new GraphAxesResponseDTO(
                axes.getId(),
                axes.getPattern().getId(),
                axes.getAbscissaPx(),
                axes.getAbscissaGridLinesEnable(),
                axes.getPrimaryOrdinatePx(),
                axes.getSecondaryOrdinatePx(),
                axes.getPrimaryOrdinateGridLinesEnable(),
                axes.getPrimaryOrdinateTitle(),
                axes.getSecondaryOrdinateTitle(),
                axes.getPrimaryOrdinateSpacing(),
                axes.getSecondaryOrdinateSpacing(),
                axes.getPrimaryOrdinateInitialValue(),
                axes.getSecondaryOrdinateInitialValue(),
                axes.getPrimaryOrdinateMaximumValue(),
                axes.getSecondaryOrdinateMaximumValue()
        );
    }
}
