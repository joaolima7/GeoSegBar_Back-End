package com.geosegbar.infra.instrument_graph_customization_properties.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.constant.services.ConstantService;
import com.geosegbar.infra.deterministic_limit.services.DeterministicLimitService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.instrument.services.InstrumentService;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.GraphPropertiesResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.PropertyResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdateGraphPropertiesRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdateGraphPropertiesRequestDTO.DeterministicLimitValueReference;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdateGraphPropertiesRequestDTO.StatisticalLimitValueReference;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertiesBatchRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertiesBatchResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertyRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.persistence.jpa.InstrumentGraphCustomizationPropertiesRepository;
import com.geosegbar.infra.instrument_graph_pattern.services.InstrumentGraphPatternService;
import com.geosegbar.infra.output.persistence.jpa.OutputRepository;
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
    private final InstrumentRepository instrumentRepository;
    private final OutputRepository outputRepository;
    private final StatisticalLimitService statLimitService;
    private final DeterministicLimitService detLimitService;
    private final ConstantService constantService;

    @Transactional
    @Caching(evict = {
        @CacheEvict(
                value = "graphPatternById",
                key = "#patternId",
                cacheManager = "instrumentGraphCacheManager"
        ),
        @CacheEvict(
                value = "graphProperties",
                key = "'pattern-' + #patternId",
                cacheManager = "instrumentGraphCacheManager"
        ),
        @CacheEvict(
                value = "graphProperties",
                key = "'pattern-properties-' + #patternId",
                cacheManager = "instrumentGraphCacheManager"
        ),
        @CacheEvict(
                value = {"folderWithPatterns", "damFoldersWithPatterns", "graphPatternsByInstrument"},
                allEntries = true,
                cacheManager = "instrumentGraphCacheManager"
        )
    })
    public void updateProperties(Long patternId, UpdateGraphPropertiesRequestDTO req) {

        InstrumentGraphPatternEntity pattern = patternService.findById(patternId);

        List<InstrumentGraphCustomizationPropertiesEntity> existingProperties = propertiesRepository.findByPatternId(patternId);

        Long damId = pattern.getInstrument().getDam().getId();

        validateAllElementsBelongToDam(
                req.getInstrumentIds(),
                req.getOutputIds(),
                req.getStatisticalLimitValues().stream().map(StatisticalLimitValueReference::getLimitId).toList(),
                req.getDeterministicLimitValues().stream().map(DeterministicLimitValueReference::getLimitId).toList(),
                req.getConstantIds(),
                damId
        );

        manageInstrumentProperties(pattern, existingProperties,
                getExistingInstrumentIds(existingProperties),
                new HashSet<>(req.getInstrumentIds()));

        manageOutputProperties(pattern, existingProperties,
                getExistingOutputIds(existingProperties),
                new HashSet<>(req.getOutputIds()));

        manageConstantProperties(pattern, existingProperties,
                getExistingConstantIds(existingProperties),
                new HashSet<>(req.getConstantIds()));

        manageStatisticalLimitValueProperties(pattern, existingProperties, req.getStatisticalLimitValues());

        manageDeterministicLimitValueProperties(pattern, existingProperties, req.getDeterministicLimitValues());

        log.info("Propriedades atualizadas para pattern: {}", patternId);
    }

    private void validateAllElementsBelongToDam(
            List<Long> instrumentIds,
            List<Long> outputIds,
            List<Long> statisticalLimitIds,
            List<Long> deterministicLimitIds,
            List<Long> constantIds,
            Long damId) {

        if (instrumentIds != null && !instrumentIds.isEmpty()) {
            List<Long> validInstrumentIds = instrumentRepository.findInstrumentIdsByDamId(damId)
                    .stream()
                    .filter(instrumentIds::contains)
                    .toList();

            List<Long> invalidInstrumentIds = instrumentIds.stream()
                    .filter(id -> !validInstrumentIds.contains(id))
                    .toList();

            if (!invalidInstrumentIds.isEmpty()) {
                throw new InvalidInputException("Os seguintes instrumentos não pertencem à mesma barragem do padrão: " + invalidInstrumentIds);
            }
        }

        if (outputIds != null && !outputIds.isEmpty()) {
            List<Long> validOutputIds = outputRepository.findOutputIdsByInstrumentDamId(damId)
                    .stream()
                    .filter(outputIds::contains)
                    .toList();

            List<Long> invalidOutputIds = outputIds.stream()
                    .filter(id -> !validOutputIds.contains(id))
                    .toList();

            if (!invalidOutputIds.isEmpty()) {
                throw new InvalidInputException("Os seguintes outputs não pertencem à mesma barragem do padrão: " + invalidOutputIds);
            }
        }

        if (constantIds != null && !constantIds.isEmpty()) {
            List<Long> validConstantIds = constantService.findConstantIdsByInstrumentDamId(damId)
                    .stream()
                    .filter(constantIds::contains)
                    .toList();

            List<Long> invalidConstantIds = constantIds.stream()
                    .filter(id -> !validConstantIds.contains(id))
                    .toList();

            if (!invalidConstantIds.isEmpty()) {
                throw new InvalidInputException("As seguintes constantes não pertencem à barragem selecionada: " + invalidConstantIds);
            }
        }

        if (statisticalLimitIds != null && !statisticalLimitIds.isEmpty()) {
            List<Long> validLimitIds = statLimitService.findStatisticalLimitIdsByOutputInstrumentDamId(damId)
                    .stream()
                    .filter(statisticalLimitIds::contains)
                    .toList();

            List<Long> invalidLimitIds = statisticalLimitIds.stream()
                    .filter(id -> !validLimitIds.contains(id))
                    .toList();

            if (!invalidLimitIds.isEmpty()) {
                throw new InvalidInputException("Os seguintes limites estatísticos não pertencem à mesma barragem do padrão: " + invalidLimitIds);
            }
        }

        if (deterministicLimitIds != null && !deterministicLimitIds.isEmpty()) {
            List<Long> validLimitIds = detLimitService.findDeterministicLimitIdsByOutputInstrumentDamId(damId)
                    .stream()
                    .filter(deterministicLimitIds::contains)
                    .toList();

            List<Long> invalidLimitIds = deterministicLimitIds.stream()
                    .filter(id -> !validLimitIds.contains(id))
                    .toList();

            if (!invalidLimitIds.isEmpty()) {
                throw new InvalidInputException("Os seguintes limites determinísticos não pertencem à mesma barragem do padrão: " + invalidLimitIds);
            }
        }
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "graphPatternById", key = "#result.patternId", cacheManager = "instrumentGraphCacheManager"),
        @CacheEvict(value = "graphProperties", key = "'pattern-' + #result.patternId", cacheManager = "instrumentGraphCacheManager"),
        @CacheEvict(value = "graphProperties", key = "'pattern-properties-' + #result.patternId", cacheManager = "instrumentGraphCacheManager"),
        @CacheEvict(value = "graphProperties", key = "'property-' + #propertyId", cacheManager = "instrumentGraphCacheManager"),
        @CacheEvict(value = {"folderWithPatterns", "damFoldersWithPatterns", "graphPatternsByInstrument"}, allEntries = true, cacheManager = "instrumentGraphCacheManager")
    })
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

    @Transactional
    public UpdatePropertiesBatchResponseDTO updatePropertiesBatch(Long patternId, UpdatePropertiesBatchRequestDTO req) {
        patternService.findById(patternId);

        List<Long> propertyIds = req.getProperties().stream()
                .map(UpdatePropertiesBatchRequestDTO.PropertyUpdateItem::getId)
                .toList();

        List<InstrumentGraphCustomizationPropertiesEntity> existingProperties = propertiesRepository.findAllById(propertyIds);

        List<InstrumentGraphCustomizationPropertiesEntity> validProperties = existingProperties.stream()
                .filter(prop -> prop.getPattern().getId().equals(patternId))
                .toList();

        Map<Long, InstrumentGraphCustomizationPropertiesEntity> propertyMap = validProperties.stream()
                .collect(Collectors.toMap(InstrumentGraphCustomizationPropertiesEntity::getId, prop -> prop));

        List<PropertyResponseDTO> updatedProperties = new ArrayList<>();
        List<UpdatePropertiesBatchResponseDTO.PropertyUpdateError> errors = new ArrayList<>();

        Map<Long, Set<LimitValueTypeEnum>> statisticalLimitValueTypes = new HashMap<>();
        Map<Long, Set<LimitValueTypeEnum>> deterministicLimitValueTypes = new HashMap<>();

        for (UpdatePropertiesBatchRequestDTO.PropertyUpdateItem item : req.getProperties()) {
            try {
                InstrumentGraphCustomizationPropertiesEntity property = propertyMap.get(item.getId());

                if (property == null) {
                    throw new NotFoundException("Propriedade não encontrada ou não pertence ao padrão: " + item.getId());
                }

                if (item.getLimitValueType() != null && !item.getLimitValueType().equals(property.getLimitValueType())) {
                    if (property.getStatisticalLimit() != null) {
                        Long limitId = property.getStatisticalLimit().getId();
                        if (!statisticalLimitValueTypes.containsKey(limitId)) {
                            statisticalLimitValueTypes.put(limitId, new HashSet<>());
                        }
                        if (statisticalLimitValueTypes.get(limitId).contains(item.getLimitValueType())) {
                            throw new InvalidInputException("Tipo de valor duplicado para limite estatístico: " + limitId);
                        }
                        validateUniqueValueType(patternId, limitId, null, item.getLimitValueType(), property.getId());
                        statisticalLimitValueTypes.get(limitId).add(item.getLimitValueType());
                    } else if (property.getDeterministicLimit() != null) {
                        Long limitId = property.getDeterministicLimit().getId();
                        if (!deterministicLimitValueTypes.containsKey(limitId)) {
                            deterministicLimitValueTypes.put(limitId, new HashSet<>());
                        }
                        if (deterministicLimitValueTypes.get(limitId).contains(item.getLimitValueType())) {
                            throw new InvalidInputException("Tipo de valor duplicado para limite determinístico: " + limitId);
                        }
                        validateUniqueValueType(patternId, null, limitId, item.getLimitValueType(), property.getId());
                        deterministicLimitValueTypes.get(limitId).add(item.getLimitValueType());
                    }
                }

                property.setName(item.getName());
                property.setFillColor(item.getFillColor());
                property.setLineType(item.getLineType());
                property.setLabelEnable(item.getLabelEnable());
                property.setIsPrimaryOrdinate(item.getIsPrimaryOrdinate());

                if (item.getLimitValueType() != null) {
                    property.setLimitValueType(item.getLimitValueType());
                }

                updatedProperties.add(mapToPropertyResponseDTO(property));

            } catch (Exception e) {
                errors.add(new UpdatePropertiesBatchResponseDTO.PropertyUpdateError(item.getId(), e.getMessage()));
                log.error("Erro atualização em lote property {}: {}", item.getId(), e.getMessage());
            }
        }

        if (!validProperties.isEmpty()) {
            propertiesRepository.saveAll(validProperties);
        }

        return new UpdatePropertiesBatchResponseDTO(patternId, updatedProperties.size(), updatedProperties, errors);
    }

    public PropertyResponseDTO findPropertyById(Long propertyId) {

        InstrumentGraphCustomizationPropertiesEntity property = propertiesRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada com ID: " + propertyId));
        return mapToPropertyResponseDTO(property);
    }

    @Transactional(readOnly = true)
    public List<PropertyResponseDTO> findPropertiesByPatternId(Long patternId) {

        List<InstrumentGraphCustomizationPropertiesEntity> properties = propertiesRepository.findByPatternId(patternId);
        return properties.stream().map(this::mapToPropertyResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public GraphPropertiesResponseDTO findByPatternId(Long patternId) {
        patternService.findById(patternId);

        List<InstrumentGraphCustomizationPropertiesEntity> properties = propertiesRepository.findByPatternId(patternId);
        List<GraphPropertiesResponseDTO.PropertyDetailDTO> propertyDetails = properties.stream()
                .map(this::mapToPropertyDetailDTO)
                .toList();

        return new GraphPropertiesResponseDTO(patternId, propertyDetails);
    }

    private void validateUniqueValueType(Long patternId, Long statisticalLimitId, Long deterministicLimitId,
            LimitValueTypeEnum valueType, Long excludePropertyId) {
        if (valueType == null) {
            return;
        }

        List<InstrumentGraphCustomizationPropertiesEntity> existingProperties;
        if (statisticalLimitId != null) {
            existingProperties = propertiesRepository.findByPatternIdAndStatisticalLimitIdAndLimitValueType(patternId, statisticalLimitId, valueType);
        } else if (deterministicLimitId != null) {
            existingProperties = propertiesRepository.findByPatternIdAndDeterministicLimitIdAndLimitValueType(patternId, deterministicLimitId, valueType);
        } else {
            return;
        }

        boolean hasDuplicate = existingProperties.stream().anyMatch(p -> !p.getId().equals(excludePropertyId));
        if (hasDuplicate) {
            String limitType = statisticalLimitId != null ? "estatístico" : "determinístico";
            throw new InvalidInputException("Já existe uma propriedade para o limite " + limitType + " com o tipo de valor: " + valueType);
        }
    }

    private void manageStatisticalLimitValueProperties(InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            List<UpdateGraphPropertiesRequestDTO.StatisticalLimitValueReference> references) {

        Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> existingLimitProps = mapExistingStatisticalLimitProperties(existingProperties);
        Map<Long, Set<LimitValueTypeEnum>> requestedValues = new HashMap<>();

        for (UpdateGraphPropertiesRequestDTO.StatisticalLimitValueReference ref : references) {
            if (!requestedValues.containsKey(ref.getLimitId())) {
                requestedValues.put(ref.getLimitId(), new HashSet<>());
            }
            if (requestedValues.get(ref.getLimitId()).contains(ref.getValueType())) {
                throw new InvalidInputException("Valor duplicado: " + ref.getLimitId());
            }
            requestedValues.get(ref.getLimitId()).add(ref.getValueType());
        }

        for (Long limitId : existingLimitProps.keySet()) {
            Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity> valueProps = existingLimitProps.get(limitId);
            if (!requestedValues.containsKey(limitId)) {
                valueProps.values().forEach(propertiesRepository::delete);
                continue;
            }
            Set<LimitValueTypeEnum> requested = requestedValues.get(limitId);
            valueProps.forEach((type, prop) -> {
                if (!requested.contains(type)) {
                    propertiesRepository.delete(prop);
                }
            });
        }

        for (UpdateGraphPropertiesRequestDTO.StatisticalLimitValueReference ref : references) {
            boolean exists = existingLimitProps.containsKey(ref.getLimitId()) && existingLimitProps.get(ref.getLimitId()).containsKey(ref.getValueType());
            if (!exists) {
                StatisticalLimitEntity statLimit = statLimitService.findById(ref.getLimitId());
                createCustomizationProperty(pattern, CustomizationTypeEnum.STATISTICAL_LIMIT, null, statLimit, null, null, null, ref.getValueType());
            }
        }
    }

    private void manageDeterministicLimitValueProperties(InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            List<UpdateGraphPropertiesRequestDTO.DeterministicLimitValueReference> references) {

        Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> existingLimitProps = mapExistingDeterministicLimitProperties(existingProperties);
        Map<Long, Set<LimitValueTypeEnum>> requestedValues = new HashMap<>();

        for (UpdateGraphPropertiesRequestDTO.DeterministicLimitValueReference ref : references) {
            if (!requestedValues.containsKey(ref.getLimitId())) {
                requestedValues.put(ref.getLimitId(), new HashSet<>());
            }
            if (requestedValues.get(ref.getLimitId()).contains(ref.getValueType())) {
                throw new InvalidInputException("Valor duplicado: " + ref.getLimitId());
            }
            requestedValues.get(ref.getLimitId()).add(ref.getValueType());
        }

        for (Long limitId : existingLimitProps.keySet()) {
            Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity> valueProps = existingLimitProps.get(limitId);
            if (!requestedValues.containsKey(limitId)) {
                valueProps.values().forEach(propertiesRepository::delete);
                continue;
            }
            Set<LimitValueTypeEnum> requested = requestedValues.get(limitId);
            valueProps.forEach((type, prop) -> {
                if (!requested.contains(type)) {
                    propertiesRepository.delete(prop);
                }
            });
        }

        for (UpdateGraphPropertiesRequestDTO.DeterministicLimitValueReference ref : references) {
            boolean exists = existingLimitProps.containsKey(ref.getLimitId()) && existingLimitProps.get(ref.getLimitId()).containsKey(ref.getValueType());
            if (!exists) {
                DeterministicLimitEntity detLimit = detLimitService.findById(ref.getLimitId());
                createCustomizationProperty(pattern, CustomizationTypeEnum.DETERMINISTIC_LIMIT, null, null, detLimit, null, null, ref.getValueType());
            }
        }
    }

    private Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> mapExistingStatisticalLimitProperties(List<InstrumentGraphCustomizationPropertiesEntity> properties) {
        Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> result = new HashMap<>();
        for (InstrumentGraphCustomizationPropertiesEntity prop : properties) {
            if (prop.getCustomizationType() == CustomizationTypeEnum.STATISTICAL_LIMIT && prop.getStatisticalLimit() != null && prop.getLimitValueType() != null) {
                result.computeIfAbsent(prop.getStatisticalLimit().getId(), k -> new HashMap<>()).put(prop.getLimitValueType(), prop);
            }
        }
        return result;
    }

    private Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> mapExistingDeterministicLimitProperties(List<InstrumentGraphCustomizationPropertiesEntity> properties) {
        Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> result = new HashMap<>();
        for (InstrumentGraphCustomizationPropertiesEntity prop : properties) {
            if (prop.getCustomizationType() == CustomizationTypeEnum.DETERMINISTIC_LIMIT && prop.getDeterministicLimit() != null && prop.getLimitValueType() != null) {
                result.computeIfAbsent(prop.getDeterministicLimit().getId(), k -> new HashMap<>()).put(prop.getLimitValueType(), prop);
            }
        }
        return result;
    }

    private void manageInstrumentProperties(InstrumentGraphPatternEntity pattern, List<InstrumentGraphCustomizationPropertiesEntity> existingProperties, Set<Long> currentIds, Set<Long> newIds) {

        Set<Long> idsToRemove = new HashSet<>(currentIds);
        idsToRemove.removeAll(newIds);
        for (Long idToRemove : idsToRemove) {
            existingProperties.stream()
                    .filter(p -> (p.getCustomizationType() == CustomizationTypeEnum.INSTRUMENT || p.getCustomizationType() == CustomizationTypeEnum.LINIMETRIC_RULER)
                    && p.getInstrument() != null
                    && p.getInstrument().getId().equals(idToRemove))
                    .forEach(propertiesRepository::delete);
        }

        Set<Long> idsToAdd = new HashSet<>(newIds);
        idsToAdd.removeAll(currentIds);
        for (Long idToAdd : idsToAdd) {
            InstrumentEntity instrument = instrumentService.findById(idToAdd);
            CustomizationTypeEnum type = Boolean.TRUE.equals(instrument.getIsLinimetricRuler()) ? CustomizationTypeEnum.LINIMETRIC_RULER : CustomizationTypeEnum.INSTRUMENT;
            createCustomizationProperty(pattern, type, null, null, null, instrument, null, null);
        }
    }

    private void manageConstantProperties(InstrumentGraphPatternEntity pattern, List<InstrumentGraphCustomizationPropertiesEntity> existingProperties, Set<Long> currentIds, Set<Long> newIds) {
        Set<Long> idsToRemove = new HashSet<>(currentIds);
        idsToRemove.removeAll(newIds);
        for (Long idToRemove : idsToRemove) {
            existingProperties.stream().filter(p -> p.getCustomizationType() == CustomizationTypeEnum.CONSTANT && p.getConstant() != null && p.getConstant().getId().equals(idToRemove)).forEach(propertiesRepository::delete);
        }

        Set<Long> idsToAdd = new HashSet<>(newIds);
        idsToAdd.removeAll(currentIds);
        for (Long idToAdd : idsToAdd) {
            ConstantEntity constant = constantService.findById(idToAdd);
            createCustomizationProperty(pattern, CustomizationTypeEnum.CONSTANT, null, null, null, null, constant, null);
        }
    }

    private void manageOutputProperties(InstrumentGraphPatternEntity pattern, List<InstrumentGraphCustomizationPropertiesEntity> existingProperties, Set<Long> currentIds, Set<Long> newIds) {
        Set<Long> idsToRemove = new HashSet<>(currentIds);
        idsToRemove.removeAll(newIds);
        for (Long idToRemove : idsToRemove) {
            existingProperties.stream().filter(p -> p.getCustomizationType() == CustomizationTypeEnum.OUTPUT && p.getOutput() != null && p.getOutput().getId().equals(idToRemove)).forEach(propertiesRepository::delete);
        }

        Set<Long> idsToAdd = new HashSet<>(newIds);
        idsToAdd.removeAll(currentIds);
        for (Long idToAdd : idsToAdd) {
            OutputEntity output = outputService.findById(idToAdd);
            createCustomizationProperty(pattern, CustomizationTypeEnum.OUTPUT, output, null, null, null, null, null);
        }
    }

    private void createCustomizationProperty(InstrumentGraphPatternEntity pattern, CustomizationTypeEnum type, OutputEntity output, StatisticalLimitEntity statLimit, DeterministicLimitEntity detLimit, InstrumentEntity instrument, ConstantEntity constant, LimitValueTypeEnum limitValueType) {
        InstrumentGraphCustomizationPropertiesEntity property = new InstrumentGraphCustomizationPropertiesEntity();
        property.setPattern(pattern);
        property.setCustomizationType(type);
        property.setLimitValueType(limitValueType);

        property.setFillColor("#000000");
        property.setLineType(LineTypeEnum.SOLID);

        if (type == CustomizationTypeEnum.OUTPUT) {
            property.setFillColor("#FF0000");
        } else if (type == CustomizationTypeEnum.INSTRUMENT) {
            property.setFillColor("#0000FF");
        } else if (type == CustomizationTypeEnum.LINIMETRIC_RULER) {
            property.setFillColor("#00FFFF");
        } else if (type == CustomizationTypeEnum.CONSTANT) {
            property.setFillColor("#800080");
        } else if (limitValueType != null) {
            property.setLineType(LineTypeEnum.DASHED);
            if (limitValueType == LimitValueTypeEnum.STATISTICAL_LOWER) {
                property.setFillColor("#00AA00");
            } else if (limitValueType == LimitValueTypeEnum.STATISTICAL_UPPER) {
                property.setFillColor("#AA0000");
            } else if (limitValueType == LimitValueTypeEnum.DETERMINISTIC_ATTENTION) {
                property.setFillColor("#FFFF00");
            } else if (limitValueType == LimitValueTypeEnum.DETERMINISTIC_ALERT) {
                property.setFillColor("#FFA500");
            } else if (limitValueType == LimitValueTypeEnum.DETERMINISTIC_EMERGENCY) {
                property.setFillColor("#FF0000");
            }
        }

        property.setLabelEnable(true);
        property.setIsPrimaryOrdinate(true);

        if (output != null) {
            property.setOutput(output);
        }
        if (statLimit != null) {
            property.setStatisticalLimit(statLimit);
        }
        if (detLimit != null) {
            property.setDeterministicLimit(detLimit);
        }
        if (instrument != null) {
            property.setInstrument(instrument);
        }
        if (constant != null) {
            property.setConstant(constant);
        }

        propertiesRepository.save(property);
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
        dto.setLimitValueType(property.getLimitValueType());

        if (property.getInstrument() != null) {
            dto.setInstrumentId(property.getInstrument().getId());
        }
        if (property.getOutput() != null) {
            dto.setOutputId(property.getOutput().getId());
        }
        if (property.getConstant() != null) {
            dto.setConstantId(property.getConstant().getId());
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
        dto.setLimitValueType(property.getLimitValueType());

        if (property.getInstrument() != null) {
            dto.setInstrument(new GraphPropertiesResponseDTO.InstrumentDetailDTO(
                    property.getInstrument().getId(),
                    property.getInstrument().getName(),
                    property.getInstrument().getLocation()
            ));
        }

        if (property.getOutput() != null) {
            GraphPropertiesResponseDTO.MeasurementUnitDTO muDto = null;
            if (property.getOutput().getMeasurementUnit() != null) {
                muDto = new GraphPropertiesResponseDTO.MeasurementUnitDTO(
                        property.getOutput().getMeasurementUnit().getId(),
                        property.getOutput().getMeasurementUnit().getName(),
                        property.getOutput().getMeasurementUnit().getAcronym()
                );
            }
            dto.setOutput(new GraphPropertiesResponseDTO.OutputDetailDTO(
                    property.getOutput().getId(),
                    property.getOutput().getAcronym(),
                    property.getOutput().getName(),
                    muDto
            ));
        }

        if (property.getConstant() != null) {
            GraphPropertiesResponseDTO.MeasurementUnitDTO muDto = null;
            if (property.getConstant().getMeasurementUnit() != null) {
                muDto = new GraphPropertiesResponseDTO.MeasurementUnitDTO(
                        property.getConstant().getMeasurementUnit().getId(),
                        property.getConstant().getMeasurementUnit().getName(),
                        property.getConstant().getMeasurementUnit().getAcronym()
                );
            }
            dto.setConstant(new GraphPropertiesResponseDTO.ConstantDetailDTO(
                    property.getConstant().getId(),
                    property.getConstant().getAcronym(),
                    property.getConstant().getName(),
                    property.getConstant().getValue(),
                    muDto
            ));
        }

        if (property.getStatisticalLimit() != null) {
            StatisticalLimitEntity statLimit = property.getStatisticalLimit();
            GraphPropertiesResponseDTO.OutputDetailDTO outputDto = null;
            if (statLimit.getOutput() != null) {
                GraphPropertiesResponseDTO.MeasurementUnitDTO muDto = null;
                if (statLimit.getOutput().getMeasurementUnit() != null) {
                    muDto = new GraphPropertiesResponseDTO.MeasurementUnitDTO(
                            statLimit.getOutput().getMeasurementUnit().getId(),
                            statLimit.getOutput().getMeasurementUnit().getName(),
                            statLimit.getOutput().getMeasurementUnit().getAcronym()
                    );
                }
                outputDto = new GraphPropertiesResponseDTO.OutputDetailDTO(
                        statLimit.getOutput().getId(),
                        statLimit.getOutput().getAcronym(),
                        statLimit.getOutput().getName(),
                        muDto
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
                GraphPropertiesResponseDTO.MeasurementUnitDTO muDto = null;
                if (detLimit.getOutput().getMeasurementUnit() != null) {
                    muDto = new GraphPropertiesResponseDTO.MeasurementUnitDTO(
                            detLimit.getOutput().getMeasurementUnit().getId(),
                            detLimit.getOutput().getMeasurementUnit().getName(),
                            detLimit.getOutput().getMeasurementUnit().getAcronym()
                    );
                }
                outputDto = new GraphPropertiesResponseDTO.OutputDetailDTO(
                        detLimit.getOutput().getId(),
                        detLimit.getOutput().getAcronym(),
                        detLimit.getOutput().getName(),
                        muDto
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

    private Set<Long> getExistingInstrumentIds(List<InstrumentGraphCustomizationPropertiesEntity> properties) {
        return properties.stream()
                .filter(p -> (p.getCustomizationType() == CustomizationTypeEnum.INSTRUMENT || p.getCustomizationType() == CustomizationTypeEnum.LINIMETRIC_RULER) && p.getInstrument() != null)
                .map(p -> p.getInstrument().getId())
                .collect(Collectors.toSet());
    }

    private Set<Long> getExistingOutputIds(List<InstrumentGraphCustomizationPropertiesEntity> properties) {
        return properties.stream()
                .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.OUTPUT && p.getOutput() != null)
                .map(p -> p.getOutput().getId())
                .collect(Collectors.toSet());
    }

    private Set<Long> getExistingConstantIds(List<InstrumentGraphCustomizationPropertiesEntity> properties) {
        return properties.stream()
                .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.CONSTANT && p.getConstant() != null)
                .map(p -> p.getConstant().getId())
                .collect(Collectors.toSet());
    }
}
