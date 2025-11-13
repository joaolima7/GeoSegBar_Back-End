package com.geosegbar.infra.instrument_graph_pattern.services;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphAxesEntity;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.InstrumentGraphPatternFolder;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.instrument_graph_axes.persistence.jpa.InstrumentGraphAxesRepository;
import com.geosegbar.infra.instrument_graph_pattern.dtos.CreateGraphPatternRequest;
import com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternDetailResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern.persistence.jpa.InstrumentGraphPatternRepository;
import com.geosegbar.infra.instrument_graph_pattern_folder.persistence.jpa.InstrumentGraphPatternFolderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentGraphPatternService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentGraphPatternRepository patternRepository;
    private final InstrumentGraphAxesRepository axesRepository;
    private final InstrumentGraphPatternFolderRepository folderRepository;
    private final DamService damService;

    @Cacheable(value = "graphPatternsByInstrument", key = "#instrumentId", cacheManager = "instrumentGraphCacheManager")
    public List<GraphPatternResponseDTO> findByInstrument(Long instrumentId) {
        return patternRepository.findByInstrumentId(instrumentId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "graphPatternsByInstrument", key = "'details-' + #instrumentId", cacheManager = "instrumentGraphCacheManager")
    public List<GraphPatternDetailResponseDTO> findByInstrumentWithDetails(Long instrumentId) {
        return patternRepository.findByInstrumentIdWithAllDetails(instrumentId)
                .stream()
                .map(this::mapToDetailResponseDTO)
                .collect(Collectors.toList());
    }

    public InstrumentGraphPatternEntity findById(Long id) {
        return patternRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Padrão de Gráfico não encontrado com ID: " + id + "."));
    }

    @Cacheable(value = "graphPatternsByDam", key = "#damId", cacheManager = "instrumentGraphCacheManager")
    public List<GraphPatternDetailResponseDTO> findAllPatternsByDam(Long damId) {

        damService.findById(damId);

        List<InstrumentGraphPatternEntity> patterns = patternRepository.findByInstrumentDamIdWithAllDetails(damId);

        List<GraphPatternDetailResponseDTO> patternDTOs = patterns.stream()
                .map(this::mapToDetailResponseDTO)
                .collect(Collectors.toList());

        log.info("Patterns de barragem obtidos: damId={}, totalPatterns={}", damId, patternDTOs.size());

        return patternDTOs;
    }

    @Caching(evict = {
        @CacheEvict(
                value = "graphPatternById",
                key = "#id",
                cacheManager = "instrumentGraphCacheManager"
        ),

        @CacheEvict(
                value = {"graphPatternsByInstrument", "graphPatternsByDam",
                    "folderWithPatterns", "damFoldersWithPatterns"},
                allEntries = true,
                cacheManager = "instrumentGraphCacheManager"
        )
    })
    public GraphPatternDetailResponseDTO updateNameGraphPattern(Long id, String newName) {
        InstrumentGraphPatternEntity pattern = findById(id);
        if (patternRepository.existsByNameAndInstrumentId(newName, pattern.getInstrument().getId())) {
            throw new DuplicateResourceException(
                    "Já existe um Padrão de Gráfico com o nome '" + newName + "' para este instrumento!");
        }
        pattern.setName(newName);
        return mapToDetailResponseDTO(patternRepository.save(pattern));
    }

    @Cacheable(value = "graphPatternById", key = "#id", cacheManager = "instrumentGraphCacheManager")
    public GraphPatternDetailResponseDTO findByIdWithDetails(Long id) {
        InstrumentGraphPatternEntity pattern = patternRepository.findByIdWithAllDetails(id)
                .orElseThrow(() -> new NotFoundException("Padrão de Gráfico não encontrado com ID: " + id + "."));

        return mapToDetailResponseDTO(pattern);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(
                value = "graphPatternById",
                key = "#patternId",
                cacheManager = "instrumentGraphCacheManager"
        ),

        @CacheEvict(
                value = {"graphAxes", "graphProperties"},
                key = "#patternId",
                cacheManager = "instrumentGraphCacheManager"
        ),

        @CacheEvict(
                value = {"graphPatternsByInstrument", "graphPatternsByDam",
                    "folderWithPatterns", "damFoldersWithPatterns"},
                allEntries = true,
                cacheManager = "instrumentGraphCacheManager"
        )
    })
    public void deleteById(Long patternId) {
        findById(patternId);
        patternRepository.deleteById(patternId);
        log.info("Pattern excluído: id={}", patternId);
    }

    @Transactional
    @CacheEvict(
            value = {"graphPatternsByInstrument", "graphPatternsByDam",
                "folderWithPatterns", "damFoldersWithPatterns"},
            allEntries = true,
            cacheManager = "instrumentGraphCacheManager"
    )
    public GraphPatternResponseDTO create(CreateGraphPatternRequest request) {
        if (patternRepository.existsByNameAndInstrumentId(request.getName(), request.getInstrumentId())) {
            throw new DuplicateResourceException(
                    "Já existe um Padrão de Gráfico com o nome '" + request.getName() + "' para este instrumento!");
        }

        InstrumentEntity instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + request.getInstrumentId()));
        InstrumentGraphPatternFolder folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new NotFoundException("Pasta não encontrada com ID: " + request.getFolderId()));
        }

        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setName(request.getName());
        pattern.setInstrument(instrument);
        pattern.setFolder(folder);
        pattern = patternRepository.save(pattern);

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPattern(pattern);
        axes.setAbscissaPx(14);
        axes.setPrimaryOrdinatePx(14);
        axes.setSecondaryOrdinatePx(14);
        axes.setAbscissaGridLinesEnable(true);
        axes.setPrimaryOrdinateGridLinesEnable(true);
        axesRepository.save(axes);
        pattern.setAxes(axes);

        patternRepository.save(pattern);

        log.info("Pattern criado: id={}, name={}, instrumentId={}, folderId={}",
                pattern.getId(), pattern.getName(), instrument.getId(),
                folder != null ? folder.getId() : null);

        return mapToResponseDTO(pattern);
    }

    public GraphPatternResponseDTO mapToResponseDTO(InstrumentGraphPatternEntity pattern) {
        GraphPatternResponseDTO dto = new GraphPatternResponseDTO();
        dto.setId(pattern.getId());
        dto.setName(pattern.getName());
        dto.setInstrumentId(pattern.getInstrument().getId());

        if (pattern.getFolder() != null) {
            dto.setFolder(new GraphPatternResponseDTO.FolderSummaryDTO(
                    pattern.getFolder().getId(),
                    pattern.getFolder().getName()
            ));
        }

        return dto;
    }

    public GraphPatternDetailResponseDTO mapToDetailResponseDTO(InstrumentGraphPatternEntity pattern) {

        final InstrumentGraphPatternEntity finalPattern;

        if (!Hibernate.isInitialized(pattern.getProperties())) {
            finalPattern = patternRepository.findByIdWithAllDetails(pattern.getId())
                    .orElse(pattern);
        } else {
            finalPattern = pattern;
        }

        GraphPatternDetailResponseDTO dto = new GraphPatternDetailResponseDTO();
        dto.setId(finalPattern.getId());
        dto.setName(finalPattern.getName());

        if (finalPattern.getInstrument() != null) {
            dto.setInstrument(new GraphPatternDetailResponseDTO.InstrumentDetailDTO(
                    finalPattern.getInstrument().getId(),
                    finalPattern.getInstrument().getName(),
                    finalPattern.getInstrument().getLocation()
            ));
        }

        if (finalPattern.getFolder() != null) {
            dto.setFolder(new GraphPatternDetailResponseDTO.FolderDetailDTO(
                    finalPattern.getFolder().getId(),
                    finalPattern.getFolder().getName()
            ));
        }

        if (finalPattern.getAxes() != null) {
            InstrumentGraphAxesEntity axes = finalPattern.getAxes();
            dto.setAxes(new GraphPatternDetailResponseDTO.AxesDetailDTO(
                    axes.getId(),
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
            ));
        }

        if (finalPattern.getProperties() != null) {
            List<GraphPatternDetailResponseDTO.PropertyDetailDTO> properties = finalPattern.getProperties().stream()
                    .map(property -> mapToPropertyDetailDTO(property, finalPattern.getInstrument()))
                    .collect(Collectors.toList());
            dto.setProperties(properties);
        }

        return dto;
    }

    private GraphPatternDetailResponseDTO.PropertyDetailDTO mapToPropertyDetailDTO(
            InstrumentGraphCustomizationPropertiesEntity property,
            InstrumentEntity instrument) {

        GraphPatternDetailResponseDTO.PropertyDetailDTO dto = new GraphPatternDetailResponseDTO.PropertyDetailDTO();
        dto.setId(property.getId());
        dto.setName(property.getName());
        dto.setCustomizationType(property.getCustomizationType());
        dto.setFillColor(property.getFillColor());
        dto.setLineType(property.getLineType());
        dto.setLabelEnable(property.getLabelEnable());
        dto.setIsPrimaryOrdinate(property.getIsPrimaryOrdinate());
        dto.setLimitValueType(property.getLimitValueType());

        if (property.getInstrument() != null) {
            dto.setInstrument(new GraphPatternDetailResponseDTO.RelatedInstrumentDTO(
                    property.getInstrument().getId(),
                    property.getInstrument().getName(),
                    property.getInstrument().getLocation()
            ));
        }

        if (property.getOutput() != null) {
            GraphPatternDetailResponseDTO.MeasurementUnitDTO measurementUnitDTO = null;
            if (property.getOutput().getMeasurementUnit() != null) {
                measurementUnitDTO = new GraphPatternDetailResponseDTO.MeasurementUnitDTO(
                        property.getOutput().getMeasurementUnit().getId(),
                        property.getOutput().getMeasurementUnit().getName(),
                        property.getOutput().getMeasurementUnit().getAcronym()
                );
            }

            dto.setOutput(new GraphPatternDetailResponseDTO.RelatedOutputDTO(
                    property.getOutput().getId(),
                    property.getOutput().getAcronym(),
                    property.getOutput().getName(),
                    measurementUnitDTO
            ));
        }

        if (property.getConstant() != null) {
            GraphPatternDetailResponseDTO.MeasurementUnitDTO measurementUnitDTO = null;
            if (property.getConstant().getMeasurementUnit() != null) {
                measurementUnitDTO = new GraphPatternDetailResponseDTO.MeasurementUnitDTO(
                        property.getConstant().getMeasurementUnit().getId(),
                        property.getConstant().getMeasurementUnit().getName(),
                        property.getConstant().getMeasurementUnit().getAcronym()
                );
            }

            dto.setConstant(new GraphPatternDetailResponseDTO.RelatedConstantDTO(
                    property.getConstant().getId(),
                    property.getConstant().getAcronym(),
                    property.getConstant().getName(),
                    property.getConstant().getValue(),
                    measurementUnitDTO
            ));
        }

        if (property.getStatisticalLimit() != null) {
            var statLimit = property.getStatisticalLimit();
            GraphPatternDetailResponseDTO.RelatedOutputDTO outputDto = null;
            if (statLimit.getOutput() != null) {
                GraphPatternDetailResponseDTO.MeasurementUnitDTO measurementUnitDTO = null;
                if (statLimit.getOutput().getMeasurementUnit() != null) {
                    measurementUnitDTO = new GraphPatternDetailResponseDTO.MeasurementUnitDTO(
                            statLimit.getOutput().getMeasurementUnit().getId(),
                            statLimit.getOutput().getMeasurementUnit().getName(),
                            statLimit.getOutput().getMeasurementUnit().getAcronym()
                    );
                }

                outputDto = new GraphPatternDetailResponseDTO.RelatedOutputDTO(
                        statLimit.getOutput().getId(),
                        statLimit.getOutput().getAcronym(),
                        statLimit.getOutput().getName(),
                        measurementUnitDTO
                );
            }
            dto.setStatisticalLimit(new GraphPatternDetailResponseDTO.RelatedStatisticalLimitDTO(
                    statLimit.getId(),
                    statLimit.getLowerValue(),
                    statLimit.getUpperValue(),
                    outputDto
            ));
        }

        if (property.getDeterministicLimit() != null) {
            var detLimit = property.getDeterministicLimit();
            GraphPatternDetailResponseDTO.RelatedOutputDTO outputDto = null;
            if (detLimit.getOutput() != null) {
                GraphPatternDetailResponseDTO.MeasurementUnitDTO measurementUnitDTO = null;
                if (detLimit.getOutput().getMeasurementUnit() != null) {
                    measurementUnitDTO = new GraphPatternDetailResponseDTO.MeasurementUnitDTO(
                            detLimit.getOutput().getMeasurementUnit().getId(),
                            detLimit.getOutput().getMeasurementUnit().getName(),
                            detLimit.getOutput().getMeasurementUnit().getAcronym()
                    );
                }

                outputDto = new GraphPatternDetailResponseDTO.RelatedOutputDTO(
                        detLimit.getOutput().getId(),
                        detLimit.getOutput().getAcronym(),
                        detLimit.getOutput().getName(),
                        measurementUnitDTO
                );
            }
            dto.setDeterministicLimit(new GraphPatternDetailResponseDTO.RelatedDeterministicLimitDTO(
                    detLimit.getId(),
                    detLimit.getAttentionValue(),
                    detLimit.getAlertValue(),
                    detLimit.getEmergencyValue(),
                    outputDto
            ));
        }

        return dto;
    }
}
