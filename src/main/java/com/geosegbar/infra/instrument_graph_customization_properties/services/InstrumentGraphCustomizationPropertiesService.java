package com.geosegbar.infra.instrument_graph_customization_properties.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
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

    @Transactional
    public void updateProperties(Long patternId, UpdateGraphPropertiesRequestDTO req) {
        InstrumentGraphPatternEntity pattern = patternService.findById(patternId);
        List<InstrumentGraphCustomizationPropertiesEntity> existingProperties = propertiesRepository.findByPatternId(patternId);

        // Obter a barragem associada ao padrão para validação
        Long damId = pattern.getInstrument().getDam().getId();

        // Verificar se todos os elementos pertencem à mesma barragem
        validateAllElementsBelongToDam(
                req.getInstrumentIds(),
                req.getOutputIds(),
                req.getStatisticalLimitValues().stream().map(StatisticalLimitValueReference::getLimitId).toList(),
                req.getDeterministicLimitValues().stream().map(DeterministicLimitValueReference::getLimitId).toList(),
                damId
        );

        // Continuar com o código original
        manageInstrumentProperties(pattern, existingProperties,
                getExistingInstrumentIds(existingProperties),
                new HashSet<>(req.getInstrumentIds()));

        manageOutputProperties(pattern, existingProperties,
                getExistingOutputIds(existingProperties),
                new HashSet<>(req.getOutputIds()));

        manageStatisticalLimitValueProperties(pattern, existingProperties, req.getStatisticalLimitValues());

        manageDeterministicLimitValueProperties(pattern, existingProperties, req.getDeterministicLimitValues());

        manageLinimetricRulerProperty(pattern, existingProperties,
                hasLinimetricRuler(existingProperties),
                req.getLinimetricRulerEnable());

        log.info("Propriedades atualizadas para pattern: {}", patternId);
    }

    private void validateAllElementsBelongToDam(
            List<Long> instrumentIds,
            List<Long> outputIds,
            List<Long> statisticalLimitIds,
            List<Long> deterministicLimitIds,
            Long damId) {

        if (instrumentIds != null && !instrumentIds.isEmpty()) {
            // Buscar apenas os IDs de instrumentos que pertencem à barragem correta
            List<Long> validInstrumentIds = instrumentRepository.findInstrumentIdsByDamId(damId)
                    .stream()
                    .filter(instrumentIds::contains)
                    .toList();

            // Identificar IDs inválidos (que estão em instrumentIds mas não em validInstrumentIds)
            List<Long> invalidInstrumentIds = instrumentIds.stream()
                    .filter(id -> !validInstrumentIds.contains(id))
                    .toList();

            if (!invalidInstrumentIds.isEmpty()) {
                throw new InvalidInputException("Os seguintes instrumentos não pertencem à mesma barragem do padrão: " + invalidInstrumentIds);
            }
        }

        if (outputIds != null && !outputIds.isEmpty()) {
            // Buscar apenas os IDs de outputs que pertencem a instrumentos da barragem correta
            List<Long> validOutputIds = outputRepository.findOutputIdsByInstrumentDamId(damId)
                    .stream()
                    .filter(outputIds::contains)
                    .toList();

            // Identificar IDs inválidos
            List<Long> invalidOutputIds = outputIds.stream()
                    .filter(id -> !validOutputIds.contains(id))
                    .toList();

            if (!invalidOutputIds.isEmpty()) {
                throw new InvalidInputException("Os seguintes outputs não pertencem à mesma barragem do padrão: " + invalidOutputIds);
            }
        }

        if (statisticalLimitIds != null && !statisticalLimitIds.isEmpty()) {
            // Buscar apenas os IDs de limites estatísticos associados a outputs de instrumentos da barragem correta
            List<Long> validLimitIds = statLimitService.findStatisticalLimitIdsByOutputInstrumentDamId(damId)
                    .stream()
                    .filter(statisticalLimitIds::contains)
                    .toList();

            // Identificar IDs inválidos
            List<Long> invalidLimitIds = statisticalLimitIds.stream()
                    .filter(id -> !validLimitIds.contains(id))
                    .toList();

            if (!invalidLimitIds.isEmpty()) {
                throw new InvalidInputException("Os seguintes limites estatísticos não pertencem à mesma barragem do padrão: " + invalidLimitIds);
            }
        }

        if (deterministicLimitIds != null && !deterministicLimitIds.isEmpty()) {
            // Buscar apenas os IDs de limites determinísticos associados a outputs de instrumentos da barragem correta
            List<Long> validLimitIds = detLimitService.findDeterministicLimitIdsByOutputInstrumentDamId(damId)
                    .stream()
                    .filter(deterministicLimitIds::contains)
                    .toList();

            // Identificar IDs inválidos
            List<Long> invalidLimitIds = deterministicLimitIds.stream()
                    .filter(id -> !validLimitIds.contains(id))
                    .toList();

            if (!invalidLimitIds.isEmpty()) {
                throw new InvalidInputException("Os seguintes limites determinísticos não pertencem à mesma barragem do padrão: " + invalidLimitIds);
            }
        }
    }

    @Transactional
    public PropertyResponseDTO updateProperty(Long propertyId, UpdatePropertyRequestDTO req) {
        InstrumentGraphCustomizationPropertiesEntity property = propertiesRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada com ID: " + propertyId));

        // Sempre atualizar os outros campos
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
                .collect(Collectors.toMap(
                        InstrumentGraphCustomizationPropertiesEntity::getId,
                        prop -> prop
                ));

        List<PropertyResponseDTO> updatedProperties = new ArrayList<>();
        List<UpdatePropertiesBatchResponseDTO.PropertyUpdateError> errors = new ArrayList<>();

        // Manter um registro dos tipos de valores que estão sendo atualizados para validar duplicidades
        Map<Long, Set<LimitValueTypeEnum>> statisticalLimitValueTypes = new HashMap<>();
        Map<Long, Set<LimitValueTypeEnum>> deterministicLimitValueTypes = new HashMap<>();

        for (UpdatePropertiesBatchRequestDTO.PropertyUpdateItem item : req.getProperties()) {
            try {
                InstrumentGraphCustomizationPropertiesEntity property = propertyMap.get(item.getId());

                if (property == null) {
                    throw new NotFoundException("Propriedade não encontrada ou não pertence ao padrão: " + item.getId());
                }

                // Verificar se há outra propriedade com o mesmo tipo de valor no mesmo lote
                if (item.getLimitValueType() != null && !item.getLimitValueType().equals(property.getLimitValueType())) {
                    if (property.getStatisticalLimit() != null) {
                        Long limitId = property.getStatisticalLimit().getId();
                        if (!statisticalLimitValueTypes.containsKey(limitId)) {
                            statisticalLimitValueTypes.put(limitId, new HashSet<>());
                        }

                        // Verificar duplicidade no lote atual
                        if (statisticalLimitValueTypes.get(limitId).contains(item.getLimitValueType())) {
                            throw new InvalidInputException("Tipo de valor duplicado para limite estatístico: " + limitId);
                        }

                        // Verificar duplicidade no banco (exceto a própria propriedade)
                        validateUniqueValueType(patternId, limitId, null, item.getLimitValueType(), property.getId());

                        statisticalLimitValueTypes.get(limitId).add(item.getLimitValueType());
                    } else if (property.getDeterministicLimit() != null) {
                        Long limitId = property.getDeterministicLimit().getId();
                        if (!deterministicLimitValueTypes.containsKey(limitId)) {
                            deterministicLimitValueTypes.put(limitId, new HashSet<>());
                        }

                        // Verificar duplicidade no lote atual
                        if (deterministicLimitValueTypes.get(limitId).contains(item.getLimitValueType())) {
                            throw new InvalidInputException("Tipo de valor duplicado para limite determinístico: " + limitId);
                        }

                        // Verificar duplicidade no banco (exceto a própria propriedade)
                        validateUniqueValueType(patternId, null, limitId, item.getLimitValueType(), property.getId());

                        deterministicLimitValueTypes.get(limitId).add(item.getLimitValueType());
                    }
                }

                property.setName(item.getName());
                property.setFillColor(item.getFillColor());
                property.setLineType(item.getLineType());
                property.setLabelEnable(item.getLabelEnable());
                property.setIsPrimaryOrdinate(item.getIsPrimaryOrdinate());
                property.setLimitValueType(item.getLimitValueType());

                updatedProperties.add(mapToPropertyResponseDTO(property));

            } catch (NotFoundException | InvalidInputException e) {
                errors.add(new UpdatePropertiesBatchResponseDTO.PropertyUpdateError(
                        item.getId(), e.getMessage()));
                log.error("Erro ao atualizar propriedade em lote: {}", e.getMessage(), e);
            } catch (Exception e) {
                errors.add(new UpdatePropertiesBatchResponseDTO.PropertyUpdateError(
                        item.getId(), "Erro interno: " + e.getMessage()));
                log.error("Erro interno ao atualizar propriedade em lote: {}", e.getMessage(), e);
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

    public PropertyResponseDTO findPropertyById(Long propertyId) {
        InstrumentGraphCustomizationPropertiesEntity property = propertiesRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada com ID: " + propertyId));

        return mapToPropertyResponseDTO(property);
    }

    public List<PropertyResponseDTO> findPropertiesByPatternId(Long patternId) {
        List<InstrumentGraphCustomizationPropertiesEntity> properties = propertiesRepository.findByPatternId(patternId);

        return properties.stream()
                .map(this::mapToPropertyResponseDTO)
                .toList();
    }

    public GraphPropertiesResponseDTO findByPatternId(Long patternId) {
        patternService.findById(patternId);
        List<InstrumentGraphCustomizationPropertiesEntity> properties = propertiesRepository.findByPatternId(patternId);
        List<GraphPropertiesResponseDTO.PropertyDetailDTO> propertyDetails = properties.stream()
                .map(this::mapToPropertyDetailDTO)
                .toList();

        return new GraphPropertiesResponseDTO(patternId, propertyDetails);
    }

    // Validação para garantir que não exista outra propriedade com o mesmo tipo de valor para o mesmo limite
    private void validateUniqueValueType(
            Long patternId,
            Long statisticalLimitId,
            Long deterministicLimitId,
            LimitValueTypeEnum valueType,
            Long excludePropertyId) {

        if (valueType == null) {
            return;
        }

        List<InstrumentGraphCustomizationPropertiesEntity> existingProperties;

        if (statisticalLimitId != null) {
            existingProperties = propertiesRepository.findByPatternIdAndStatisticalLimitIdAndLimitValueType(
                    patternId, statisticalLimitId, valueType);
        } else if (deterministicLimitId != null) {
            existingProperties = propertiesRepository.findByPatternIdAndDeterministicLimitIdAndLimitValueType(
                    patternId, deterministicLimitId, valueType);
        } else {
            return;
        }

        boolean hasDuplicate = existingProperties.stream()
                .anyMatch(p -> !p.getId().equals(excludePropertyId));

        if (hasDuplicate) {
            String limitType = statisticalLimitId != null ? "estatístico" : "determinístico";
            throw new InvalidInputException(
                    "Já existe uma propriedade para o limite " + limitType + " com o tipo de valor: " + valueType);
        }
    }

    private void manageStatisticalLimitValueProperties(
            InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            List<UpdateGraphPropertiesRequestDTO.StatisticalLimitValueReference> references) {

        // Mapear propriedades existentes por limite e tipo de valor
        Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> existingLimitProps
                = mapExistingStatisticalLimitProperties(existingProperties);

        // Mapear referências de entrada por limite e tipo de valor
        Map<Long, Set<LimitValueTypeEnum>> requestedValues = new HashMap<>();
        for (UpdateGraphPropertiesRequestDTO.StatisticalLimitValueReference ref : references) {
            if (!requestedValues.containsKey(ref.getLimitId())) {
                requestedValues.put(ref.getLimitId(), new HashSet<>());
            }

            if (requestedValues.get(ref.getLimitId()).contains(ref.getValueType())) {
                throw new InvalidInputException("Tipo de valor duplicado para limite estatístico: " + ref.getLimitId() + ", valor: " + ref.getValueType());
            }

            requestedValues.get(ref.getLimitId()).add(ref.getValueType());

            // Validar tipo de valor apropriado para limite estatístico
            if (ref.getValueType() != LimitValueTypeEnum.STATISTICAL_LOWER
                    && ref.getValueType() != LimitValueTypeEnum.STATISTICAL_UPPER) {
                throw new InvalidInputException("Tipo de valor inválido para limite estatístico: " + ref.getValueType());
            }
        }

        // Remover propriedades que não estão mais na solicitação
        for (Long limitId : existingLimitProps.keySet()) {
            Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity> valueProps = existingLimitProps.get(limitId);

            // Se o limite não foi solicitado, remover todas as propriedades
            if (!requestedValues.containsKey(limitId)) {
                for (InstrumentGraphCustomizationPropertiesEntity prop : valueProps.values()) {
                    propertiesRepository.delete(prop);
                }
                continue;
            }

            // Para limites solicitados, remover valores não solicitados
            Set<LimitValueTypeEnum> requestedTypesForLimit = requestedValues.get(limitId);
            for (LimitValueTypeEnum valueType : valueProps.keySet()) {
                if (!requestedTypesForLimit.contains(valueType)) {
                    propertiesRepository.delete(valueProps.get(valueType));
                }
            }
        }

        // Adicionar novas propriedades
        for (UpdateGraphPropertiesRequestDTO.StatisticalLimitValueReference ref : references) {
            // Verificar se a propriedade já existe
            boolean exists = existingLimitProps.containsKey(ref.getLimitId())
                    && existingLimitProps.get(ref.getLimitId()).containsKey(ref.getValueType());

            // Se não existe, criar nova propriedade
            if (!exists) {
                StatisticalLimitEntity statLimit = statLimitService.findById(ref.getLimitId());
                createCustomizationProperty(pattern, CustomizationTypeEnum.STATISTICAL_LIMIT,
                        null, statLimit, null, null, ref.getValueType());
            }
        }
    }

    private void manageDeterministicLimitValueProperties(
            InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            List<UpdateGraphPropertiesRequestDTO.DeterministicLimitValueReference> references) {

        // Mapear propriedades existentes por limite e tipo de valor
        Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> existingLimitProps
                = mapExistingDeterministicLimitProperties(existingProperties);

        // Mapear referências de entrada por limite e tipo de valor
        Map<Long, Set<LimitValueTypeEnum>> requestedValues = new HashMap<>();
        for (UpdateGraphPropertiesRequestDTO.DeterministicLimitValueReference ref : references) {
            if (!requestedValues.containsKey(ref.getLimitId())) {
                requestedValues.put(ref.getLimitId(), new HashSet<>());
            }

            if (requestedValues.get(ref.getLimitId()).contains(ref.getValueType())) {
                throw new InvalidInputException("Tipo de valor duplicado para limite determinístico: " + ref.getLimitId() + ", valor: " + ref.getValueType());
            }

            requestedValues.get(ref.getLimitId()).add(ref.getValueType());

            // Validar tipo de valor apropriado para limite determinístico
            if (ref.getValueType() != LimitValueTypeEnum.DETERMINISTIC_ATTENTION
                    && ref.getValueType() != LimitValueTypeEnum.DETERMINISTIC_ALERT
                    && ref.getValueType() != LimitValueTypeEnum.DETERMINISTIC_EMERGENCY) {
                throw new InvalidInputException("Tipo de valor inválido para limite determinístico: " + ref.getValueType());
            }
        }

        // Remover propriedades que não estão mais na solicitação
        for (Long limitId : existingLimitProps.keySet()) {
            Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity> valueProps = existingLimitProps.get(limitId);

            // Se o limite não foi solicitado, remover todas as propriedades
            if (!requestedValues.containsKey(limitId)) {
                for (InstrumentGraphCustomizationPropertiesEntity prop : valueProps.values()) {
                    propertiesRepository.delete(prop);
                }
                continue;
            }

            // Para limites solicitados, remover valores não solicitados
            Set<LimitValueTypeEnum> requestedTypesForLimit = requestedValues.get(limitId);
            for (LimitValueTypeEnum valueType : valueProps.keySet()) {
                if (!requestedTypesForLimit.contains(valueType)) {
                    propertiesRepository.delete(valueProps.get(valueType));
                }
            }
        }

        // Adicionar novas propriedades
        for (UpdateGraphPropertiesRequestDTO.DeterministicLimitValueReference ref : references) {
            // Verificar se a propriedade já existe
            boolean exists = existingLimitProps.containsKey(ref.getLimitId())
                    && existingLimitProps.get(ref.getLimitId()).containsKey(ref.getValueType());

            // Se não existe, criar nova propriedade
            if (!exists) {
                DeterministicLimitEntity detLimit = detLimitService.findById(ref.getLimitId());
                createCustomizationProperty(pattern, CustomizationTypeEnum.DETERMINISTIC_LIMIT,
                        null, null, detLimit, null, ref.getValueType());
            }
        }
    }

    private Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> mapExistingStatisticalLimitProperties(
            List<InstrumentGraphCustomizationPropertiesEntity> properties) {

        Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> result = new HashMap<>();

        for (InstrumentGraphCustomizationPropertiesEntity prop : properties) {
            if (prop.getCustomizationType() == CustomizationTypeEnum.STATISTICAL_LIMIT
                    && prop.getStatisticalLimit() != null
                    && prop.getLimitValueType() != null) {

                Long limitId = prop.getStatisticalLimit().getId();
                if (!result.containsKey(limitId)) {
                    result.put(limitId, new HashMap<>());
                }

                result.get(limitId).put(prop.getLimitValueType(), prop);
            }
        }

        return result;
    }

    private Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> mapExistingDeterministicLimitProperties(
            List<InstrumentGraphCustomizationPropertiesEntity> properties) {

        Map<Long, Map<LimitValueTypeEnum, InstrumentGraphCustomizationPropertiesEntity>> result = new HashMap<>();

        for (InstrumentGraphCustomizationPropertiesEntity prop : properties) {
            if (prop.getCustomizationType() == CustomizationTypeEnum.DETERMINISTIC_LIMIT
                    && prop.getDeterministicLimit() != null
                    && prop.getLimitValueType() != null) {

                Long limitId = prop.getDeterministicLimit().getId();
                if (!result.containsKey(limitId)) {
                    result.put(limitId, new HashMap<>());
                }

                result.get(limitId).put(prop.getLimitValueType(), prop);
            }
        }

        return result;
    }

    private void manageInstrumentProperties(
            InstrumentGraphPatternEntity pattern,
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
                    null, null, null, instrument, null);
        }
    }

    private void manageOutputProperties(
            InstrumentGraphPatternEntity pattern,
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
                    output, null, null, null, null);
        }
    }

    private void manageLinimetricRulerProperty(
            InstrumentGraphPatternEntity pattern,
            List<InstrumentGraphCustomizationPropertiesEntity> existingProperties,
            boolean currentLinimetricRuler, boolean newLinimetricRuler) {

        if (currentLinimetricRuler && !newLinimetricRuler) {
            existingProperties.stream()
                    .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.LINIMETRIC_RULER)
                    .forEach(propertiesRepository::delete);
        } else if (!currentLinimetricRuler && newLinimetricRuler) {
            createCustomizationProperty(pattern, CustomizationTypeEnum.LINIMETRIC_RULER,
                    null, null, null, null, null);
        }
    }

    private void createCustomizationProperty(
            InstrumentGraphPatternEntity pattern,
            CustomizationTypeEnum type,
            OutputEntity output,
            StatisticalLimitEntity statLimit,
            DeterministicLimitEntity detLimit,
            InstrumentEntity instrument,
            LimitValueTypeEnum limitValueType) {

        InstrumentGraphCustomizationPropertiesEntity property = new InstrumentGraphCustomizationPropertiesEntity();
        property.setName(null);
        property.setPattern(pattern);
        property.setCustomizationType(type);
        property.setLimitValueType(limitValueType);

        // Definir cores padrão baseadas no tipo de customização e no tipo de valor do limite
        if (type == CustomizationTypeEnum.STATISTICAL_LIMIT && limitValueType != null) {
            switch (limitValueType) {
                case STATISTICAL_LOWER -> {
                    property.setFillColor("#00AA00"); // Verde para limite inferior
                    property.setLineType(LineTypeEnum.DASHED);
                }
                case STATISTICAL_UPPER -> {
                    property.setFillColor("#AA0000"); // Vermelho escuro para limite superior
                    property.setLineType(LineTypeEnum.DASHED);
                }
                default -> {
                    property.setFillColor("#000000");
                    property.setLineType(LineTypeEnum.SOLID);
                }
            }
        } else if (type == CustomizationTypeEnum.DETERMINISTIC_LIMIT && limitValueType != null) {
            switch (limitValueType) {
                case DETERMINISTIC_ATTENTION -> {
                    property.setFillColor("#FFFF00"); // Amarelo para atenção
                    property.setLineType(LineTypeEnum.DASHED);
                }
                case DETERMINISTIC_ALERT -> {
                    property.setFillColor("#FFA500"); // Laranja para alerta
                    property.setLineType(LineTypeEnum.DASHED);
                }
                case DETERMINISTIC_EMERGENCY -> {
                    property.setFillColor("#FF0000"); // Vermelho para emergência
                    property.setLineType(LineTypeEnum.DASHED);
                }
                default -> {
                    property.setFillColor("#000000");
                    property.setLineType(LineTypeEnum.SOLID);
                }
            }
        } else if (type == CustomizationTypeEnum.OUTPUT) {
            property.setFillColor("#FF0000"); // Vermelho para output
            property.setLineType(LineTypeEnum.SOLID);
        } else if (type == CustomizationTypeEnum.INSTRUMENT) {
            property.setFillColor("#0000FF"); // Azul para instrumento
            property.setLineType(LineTypeEnum.SOLID);
        } else {
            property.setFillColor("#000000");
            property.setLineType(LineTypeEnum.SOLID);
        }

        property.setLabelEnable(true);
        property.setIsPrimaryOrdinate(type != CustomizationTypeEnum.LINIMETRIC_RULER);

        switch (type) {
            case OUTPUT ->
                property.setOutput(output);
            case STATISTICAL_LIMIT ->
                property.setStatisticalLimit(statLimit);
            case DETERMINISTIC_LIMIT ->
                property.setDeterministicLimit(detLimit);
            case INSTRUMENT ->
                property.setInstrument(instrument);
            case LINIMETRIC_RULER -> {
            }
        }

        propertiesRepository.save(property);
        log.info("Criada nova propriedade de customização: tipo={}, limitValueType={}, patternId={}",
                type, limitValueType, pattern.getId());
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

    // Métodos auxiliares
    private Set<Long> getExistingInstrumentIds(List<InstrumentGraphCustomizationPropertiesEntity> properties) {
        return properties.stream()
                .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.INSTRUMENT)
                .filter(p -> p.getInstrument() != null)
                .map(p -> p.getInstrument().getId())
                .collect(Collectors.toSet());
    }

    private Set<Long> getExistingOutputIds(List<InstrumentGraphCustomizationPropertiesEntity> properties) {
        return properties.stream()
                .filter(p -> p.getCustomizationType() == CustomizationTypeEnum.OUTPUT)
                .filter(p -> p.getOutput() != null)
                .map(p -> p.getOutput().getId())
                .collect(Collectors.toSet());
    }

    private boolean hasLinimetricRuler(List<InstrumentGraphCustomizationPropertiesEntity> properties) {
        return properties.stream()
                .anyMatch(p -> p.getCustomizationType() == CustomizationTypeEnum.LINIMETRIC_RULER);
    }
}
