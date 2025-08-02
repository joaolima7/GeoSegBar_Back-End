package com.geosegbar.infra.instrument_graph_customization_properties.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.deterministic_limit.services.DeterministicLimitService;
import com.geosegbar.infra.instrument.services.InstrumentService;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.GraphPropertiesResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.PropertyResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdateGraphPropertiesRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertiesBatchRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertiesBatchResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertyRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.persistence.jpa.InstrumentGraphCustomizationPropertiesRepository;
import com.geosegbar.infra.instrument_graph_pattern.services.InstrumentGraphPatternService;
import com.geosegbar.infra.output.services.OutputService;
import com.geosegbar.infra.statistical_limit.services.StatisticalLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentGraphCustomizationPropertiesService {

    private final InstrumentGraphCustomizationPropertiesRepository propertiesRepository;
    private final InstrumentGraphPatternService patternService;
    private final InstrumentService instrumentService;
    private final OutputService outputService;
    private final StatisticalLimitService statLimitService;
    private final DeterministicLimitService detLimitService;

    @Transactional
    public void updateProperties(Long patternId, UpdateGraphPropertiesRequestDTO req) {

        InstrumentGraphPatternEntity pattern = patternService.findById(patternId);

        List<InstrumentGraphCustomizationPropertiesEntity> existingProperties
                = propertiesRepository.findByPatternId(patternId);

        Set<Long> currentInstrumentIds = existingProperties.stream()
                .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.INSTRUMENT)
                .map(p -> p.getInstrument().getId())
                .collect(Collectors.toSet());

        Set<Long> currentOutputIds = existingProperties.stream()
                .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.OUTPUT)
                .map(p -> p.getOutput().getId())
                .collect(Collectors.toSet());

        Set<Long> currentStatLimitIds = existingProperties.stream()
                .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.STATISTICAL_LIMIT)
                .map(p -> p.getStatisticalLimit().getId())
                .collect(Collectors.toSet());

        Set<Long> currentDetLimitIds = existingProperties.stream()
                .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.DETERMINISTIC_LIMIT)
                .map(p -> p.getDeterministicLimit().getId())
                .collect(Collectors.toSet());

        boolean currentLinimetricRuler = existingProperties.stream()
                .anyMatch(p -> p.getCustomizationType() == CustomizationTypeEnum.LINIMETRIC_RULER);

        Set<Long> newInstrumentIds = new HashSet<>(req.getInstrumentIds());
        Set<Long> newOutputIds = new HashSet<>(req.getOutputIds());
        Set<Long> newStatLimitIds = new HashSet<>(req.getStatisticalLimitIds());
        Set<Long> newDetLimitIds = new HashSet<>(req.getDeterministicLimitIds());

        manageInstrumentProperties(pattern, existingProperties, currentInstrumentIds, newInstrumentIds);
        manageOutputProperties(pattern, existingProperties, currentOutputIds, newOutputIds);
        manageStatisticalLimitProperties(pattern, existingProperties, currentStatLimitIds, newStatLimitIds);
        manageDeterministicLimitProperties(pattern, existingProperties, currentDetLimitIds, newDetLimitIds);
        manageLinimetricRulerProperty(pattern, existingProperties, currentLinimetricRuler, req.getLinimetricRulerEnable());

        log.info("Propriedades atualizadas para pattern: {}", patternId);
    }

    @Transactional
    public PropertyResponseDTO updateProperty(Long propertyId, UpdatePropertyRequestDTO req) {
        InstrumentGraphCustomizationPropertiesEntity property = propertiesRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada com ID: " + propertyId));

        property.setName(req.getName());
        property.setFillColor(req.getFillColor());
        property.setLineType(req.getLineType());
        property.setLabelEnable(req.getLabelEnable());
        property.setIsPrimaryOrdinate(req.getIsPrimaryOrdinate());

        InstrumentGraphCustomizationPropertiesEntity savedProperty = propertiesRepository.save(property);

        return mapToPropertyResponseDTO(savedProperty);
    }

    public PropertyResponseDTO findPropertyById(Long propertyId) {
        InstrumentGraphCustomizationPropertiesEntity property = propertiesRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada com ID: " + propertyId));

        return mapToPropertyResponseDTO(property);
    }

    public List<PropertyResponseDTO> findPropertiesByPatternId(Long patternId) {
        List<InstrumentGraphCustomizationPropertiesEntity> properties
                = propertiesRepository.findByPatternId(patternId);

        return properties.stream()
                .map(this::mapToPropertyResponseDTO)
                .toList();
    }

    public GraphPropertiesResponseDTO findByPatternId(Long patternId) {

        patternService.findById(patternId);

        List<InstrumentGraphCustomizationPropertiesEntity> properties
                = propertiesRepository.findByPatternId(patternId);

        List<GraphPropertiesResponseDTO.PropertyDetailDTO> propertyDetails = properties.stream()
                .map(this::mapToPropertyDetailDTO)
                .toList();

        return new GraphPropertiesResponseDTO(patternId, propertyDetails);
    }

    private void manageInstrumentProperties(InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            Set<Long> currentIds, Set<Long> newIds) {

        Set<Long> idsToRemove = new HashSet<>(currentIds);
        idsToRemove.removeAll(newIds);

        for (Long idToRemove : idsToRemove) {
            existingProperties.stream()
                    .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.INSTRUMENT
                    && p.getInstrument() != null && p.getInstrument().getId().equals(idToRemove))
                    .forEach(propertiesRepository::delete);
        }

        Set<Long> idsToAdd = new HashSet<>(newIds);
        idsToAdd.removeAll(currentIds);

        for (Long idToAdd : idsToAdd) {
            InstrumentEntity instrument = instrumentService.findById(idToAdd);
            createCustomizationProperty(pattern, CustomizationTypeEnum.INSTRUMENT,
                    null, null, null, instrument);
        }
    }

    private void manageOutputProperties(InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            Set<Long> currentIds, Set<Long> newIds) {

        Set<Long> idsToRemove = new HashSet<>(currentIds);
        idsToRemove.removeAll(newIds);

        for (Long idToRemove : idsToRemove) {
            existingProperties.stream()
                    .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.OUTPUT
                    && p.getOutput() != null && p.getOutput().getId().equals(idToRemove))
                    .forEach(propertiesRepository::delete);
        }

        Set<Long> idsToAdd = new HashSet<>(newIds);
        idsToAdd.removeAll(currentIds);

        for (Long idToAdd : idsToAdd) {
            OutputEntity output = outputService.findById(idToAdd);
            createCustomizationProperty(pattern, CustomizationTypeEnum.OUTPUT,
                    output, null, null, null);
        }
    }

    private void manageStatisticalLimitProperties(InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            Set<Long> currentIds, Set<Long> newIds) {

        Set<Long> idsToRemove = new HashSet<>(currentIds);
        idsToRemove.removeAll(newIds);

        for (Long idToRemove : idsToRemove) {
            existingProperties.stream()
                    .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.STATISTICAL_LIMIT
                    && p.getStatisticalLimit() != null && p.getStatisticalLimit().getId().equals(idToRemove))
                    .forEach(propertiesRepository::delete);
        }

        Set<Long> idsToAdd = new HashSet<>(newIds);
        idsToAdd.removeAll(currentIds);

        for (Long idToAdd : idsToAdd) {
            StatisticalLimitEntity statLimit = statLimitService.findById(idToAdd);
            createCustomizationProperty(pattern, CustomizationTypeEnum.STATISTICAL_LIMIT,
                    null, statLimit, null, null);
        }
    }

    private void manageDeterministicLimitProperties(InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            Set<Long> currentIds, Set<Long> newIds) {

        Set<Long> idsToRemove = new HashSet<>(currentIds);
        idsToRemove.removeAll(newIds);

        for (Long idToRemove : idsToRemove) {
            existingProperties.stream()
                    .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.DETERMINISTIC_LIMIT
                    && p.getDeterministicLimit() != null && p.getDeterministicLimit().getId().equals(idToRemove))
                    .forEach(propertiesRepository::delete);
        }

        Set<Long> idsToAdd = new HashSet<>(newIds);
        idsToAdd.removeAll(currentIds);

        for (Long idToAdd : idsToAdd) {
            DeterministicLimitEntity detLimit = detLimitService.findById(idToAdd);
            createCustomizationProperty(pattern, CustomizationTypeEnum.DETERMINISTIC_LIMIT,
                    null, null, detLimit, null);
        }
    }

    private void manageLinimetricRulerProperty(InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            boolean currentLinimetricRuler, boolean newLinimetricRuler) {

        if (currentLinimetricRuler && !newLinimetricRuler) {
            existingProperties.stream()
                    .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.LINIMETRIC_RULER)
                    .forEach(propertiesRepository::delete);
        } else if (!currentLinimetricRuler && newLinimetricRuler) {
            createCustomizationProperty(pattern, CustomizationTypeEnum.LINIMETRIC_RULER,
                    null, null, null, null);
        }
    }

    private void createCustomizationProperty(InstrumentGraphPatternEntity pattern,
            CustomizationTypeEnum type, OutputEntity output, StatisticalLimitEntity statLimit,
            DeterministicLimitEntity detLimit, InstrumentEntity instrument) {

        InstrumentGraphCustomizationPropertiesEntity property = new InstrumentGraphCustomizationPropertiesEntity();
        property.setName(null);
        property.setPattern(pattern);
        property.setCustomizationType(type);
        property.setFillColor("#000000");
        property.setLineType(LineTypeEnum.SOLID);
        property.setLabelEnable(true);
        property.setIsPrimaryOrdinate(type != CustomizationTypeEnum.LINIMETRIC_RULER);

        switch (type) {
            case OUTPUT:
                property.setOutput(output);
                break;
            case STATISTICAL_LIMIT:
                property.setStatisticalLimit(statLimit);
                break;
            case DETERMINISTIC_LIMIT:
                property.setDeterministicLimit(detLimit);
                break;
            case INSTRUMENT:
                property.setInstrument(instrument);
                break;
            case LINIMETRIC_RULER:
                break;
        }

        propertiesRepository.save(property);
        log.info("Criada nova propriedade de customização: tipo={}, patternId={}",
                type, pattern.getId());
    }

    private PropertyResponseDTO mapToPropertyResponseDTO(InstrumentGraphCustomizationPropertiesEntity property) {
        PropertyResponseDTO dto = new PropertyResponseDTO();
        dto.setId(property.getId());
        dto.setName(property.getName());
        dto.setCustomizationType(property.getCustomizationType());
        dto.setFillColor(property.getFillColor());
        dto.setLineType(property.getLineType());
        dto.setLabelEnable(property.getLabelEnable());
        dto.setIsPrimaryOrdinate(property.getIsPrimaryOrdinate());
        dto.setPatternId(property.getPattern().getId());

        if (property.getInstrument() != null) {
            dto.setInstrumentId(property.getInstrument().getId());
        }
        if (property.getOutput() != null) {
            dto.setOutputId(property.getOutput().getId());
        }
        if (property.getStatisticalLimit() != null) {
            dto.setStatisticalLimitId(property.getStatisticalLimit().getId());
        }
        if (property.getDeterministicLimit() != null) {
            dto.setDeterministicLimitId(property.getDeterministicLimit().getId());
        }

        return dto;
    }

    private GraphPropertiesResponseDTO.PropertyDetailDTO mapToPropertyDetailDTO(InstrumentGraphCustomizationPropertiesEntity property) {
        GraphPropertiesResponseDTO.PropertyDetailDTO dto = new GraphPropertiesResponseDTO.PropertyDetailDTO();
        dto.setId(property.getId());
        dto.setName(property.getName());
        dto.setCustomizationType(property.getCustomizationType());
        dto.setFillColor(property.getFillColor());
        dto.setLineType(property.getLineType());
        dto.setLabelEnable(property.getLabelEnable());
        dto.setIsPrimaryOrdinate(property.getIsPrimaryOrdinate());

        if (property.getInstrument() != null) {
            dto.setInstrument(new GraphPropertiesResponseDTO.InstrumentDetailDTO(
                    property.getInstrument().getId(),
                    property.getInstrument().getName(),
                    property.getInstrument().getLocation()
            ));
        }

        if (property.getOutput() != null) {
            dto.setOutput(new GraphPropertiesResponseDTO.OutputDetailDTO(
                    property.getOutput().getId(),
                    property.getOutput().getAcronym(),
                    property.getOutput().getName()
            ));
        }

        if (property.getStatisticalLimit() != null) {
            StatisticalLimitEntity statLimit = property.getStatisticalLimit();
            GraphPropertiesResponseDTO.OutputDetailDTO outputDto = null;
            if (statLimit.getOutput() != null) {
                outputDto = new GraphPropertiesResponseDTO.OutputDetailDTO(
                        statLimit.getOutput().getId(),
                        statLimit.getOutput().getAcronym(),
                        statLimit.getOutput().getName()
                );
            }
            dto.setStatisticalLimit(new GraphPropertiesResponseDTO.StatisticalLimitDetailDTO(
                    statLimit.getId(),
                    statLimit.getLowerValue(),
                    statLimit.getUpperValue(),
                    outputDto
            ));
        }

        if (property.getDeterministicLimit() != null) {
            DeterministicLimitEntity detLimit = property.getDeterministicLimit();
            GraphPropertiesResponseDTO.OutputDetailDTO outputDto = null;
            if (detLimit.getOutput() != null) {
                outputDto = new GraphPropertiesResponseDTO.OutputDetailDTO(
                        detLimit.getOutput().getId(),
                        detLimit.getOutput().getAcronym(),
                        detLimit.getOutput().getName()
                );
            }
            dto.setDeterministicLimit(new GraphPropertiesResponseDTO.DeterministicLimitDetailDTO(
                    detLimit.getId(),
                    detLimit.getAttentionValue(),
                    detLimit.getAlertValue(),
                    detLimit.getEmergencyValue(),
                    outputDto
            ));
        }

        return dto;
    }

    @Transactional
    public UpdatePropertiesBatchResponseDTO updatePropertiesBatch(Long patternId, UpdatePropertiesBatchRequestDTO req) {

        patternService.findById(patternId);

        List<Long> propertyIds = req.getProperties().stream()
                .map(UpdatePropertiesBatchRequestDTO.PropertyUpdateItem::getId)
                .toList();

        List<InstrumentGraphCustomizationPropertiesEntity> existingProperties
                = propertiesRepository.findAllById(propertyIds);

        List<InstrumentGraphCustomizationPropertiesEntity> validProperties = existingProperties.stream()
                .filter(prop -> prop.getPattern().getId().equals(patternId))
                .toList();

        Map<Long, InstrumentGraphCustomizationPropertiesEntity> propertyMap = validProperties.stream()
                .collect(Collectors.toMap(
                        InstrumentGraphCustomizationPropertiesEntity::getId,
                        prop -> prop
                ));

        List<PropertyResponseDTO> updatedProperties = new ArrayList<>();
        List<UpdatePropertiesBatchResponseDTO.PropertyUpdateError> errors = new ArrayList<>();

        for (UpdatePropertiesBatchRequestDTO.PropertyUpdateItem item : req.getProperties()) {
            try {
                InstrumentGraphCustomizationPropertiesEntity property = propertyMap.get(item.getId());

                if (property == null) {
                    errors.add(new UpdatePropertiesBatchResponseDTO.PropertyUpdateError(
                            item.getId(),
                            "Propriedade não encontrada ou não pertence ao pattern especificado"
                    ));
                    continue;
                }

                property.setName(item.getName());
                property.setFillColor(item.getFillColor());
                property.setLineType(item.getLineType());
                property.setLabelEnable(item.getLabelEnable());
                property.setIsPrimaryOrdinate(item.getIsPrimaryOrdinate());

                updatedProperties.add(mapToPropertyResponseDTO(property));

            } catch (Exception e) {
                errors.add(new UpdatePropertiesBatchResponseDTO.PropertyUpdateError(
                        item.getId(),
                        "Erro ao atualizar propriedade: " + e.getMessage()
                ));
                log.error("Erro ao atualizar propriedade {}: {}", item.getId(), e.getMessage());
            }
        }

        if (!validProperties.isEmpty()) {
            propertiesRepository.saveAll(validProperties);
        }

        log.info("Atualização em lote concluída - Pattern: {}, Atualizadas: {}, Erros: {}",
                patternId, updatedProperties.size(), errors.size());

        return new UpdatePropertiesBatchResponseDTO(
                patternId,
                updatedProperties.size(),
                updatedProperties,
                errors
        );
    }
}
