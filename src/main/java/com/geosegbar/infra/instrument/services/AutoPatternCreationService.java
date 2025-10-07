package com.geosegbar.infra.instrument.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdateGraphPropertiesRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdateGraphPropertiesRequestDTO.DeterministicLimitValueReference;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdateGraphPropertiesRequestDTO.StatisticalLimitValueReference;
import com.geosegbar.infra.instrument_graph_customization_properties.services.InstrumentGraphCustomizationPropertiesService;
import com.geosegbar.infra.instrument_graph_pattern.dtos.CreateGraphPatternRequest;
import com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern.services.InstrumentGraphPatternService;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.CreateTabulatePatternRequestDTO;
import com.geosegbar.infra.instrument_tabulate_pattern.services.InstrumentTabulatePatternService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoPatternCreationService {

    private final InstrumentGraphPatternService graphPatternService;
    private final InstrumentTabulatePatternService tabulatePatternService;
    private final InstrumentGraphCustomizationPropertiesService propertiesService;

    @Async
    @Transactional
    public void createPatternsForInstrument(InstrumentEntity instrument) {

        if (Boolean.TRUE.equals(instrument.getIsLinimetricRuler())) {
            log.debug("Ignorando criação de padrões para régua linimétrica: {}", instrument.getId());
            return;
        }

        try {

            GraphPatternResponseDTO graphPattern = createGraphPatternForInstrument(instrument);

            if (graphPattern != null) {
                configureGraphPatternProperties(instrument, graphPattern.getId());
                log.info("Propriedades do padrão de gráfico configuradas automaticamente para o instrumento: {}", instrument.getId());
            }

            createTabulatePatternForInstrument(instrument);

            log.info("Padrões de gráfico e tabela criados com sucesso para o instrumento: {}", instrument.getId());
        } catch (Exception e) {
            log.error("Erro ao criar padrões automáticos para o instrumento {}: {}",
                    instrument.getId(), e.getMessage(), e);
        }
    }

    private GraphPatternResponseDTO createGraphPatternForInstrument(InstrumentEntity instrument) {
        String patternName = "Padrão Automático - " + instrument.getName();

        CreateGraphPatternRequest request = new CreateGraphPatternRequest();
        request.setInstrumentId(instrument.getId());
        request.setName(patternName);
        request.setFolderId(null);

        try {
            GraphPatternResponseDTO pattern = graphPatternService.create(request);
            log.debug("Padrão de gráfico criado para o instrumento: {}", instrument.getId());
            return pattern;
        } catch (Exception e) {
            log.error("Erro ao criar padrão de gráfico para o instrumento {}: {}",
                    instrument.getId(), e.getMessage(), e);
            return null;
        }
    }

    private void configureGraphPatternProperties(InstrumentEntity instrument, Long patternId) {
        try {

            List<Long> outputIds = new ArrayList<>();
            List<Long> constantIds = new ArrayList<>();
            List<StatisticalLimitValueReference> statLimits = new ArrayList<>();
            List<DeterministicLimitValueReference> detLimits = new ArrayList<>();

            for (OutputEntity output : instrument.getOutputs()) {
                if (Boolean.TRUE.equals(output.getActive())) {
                    outputIds.add(output.getId());

                    if (output.getStatisticalLimit() != null) {
                        StatisticalLimitEntity limit = output.getStatisticalLimit();
                        if (limit.getLowerValue() != null) {
                            statLimits.add(new StatisticalLimitValueReference(
                                    limit.getId(), LimitValueTypeEnum.STATISTICAL_LOWER));
                        }
                        if (limit.getUpperValue() != null) {
                            statLimits.add(new StatisticalLimitValueReference(
                                    limit.getId(), LimitValueTypeEnum.STATISTICAL_UPPER));
                        }
                    }

                    if (output.getDeterministicLimit() != null) {
                        DeterministicLimitEntity limit = output.getDeterministicLimit();
                        if (limit.getAttentionValue() != null) {
                            detLimits.add(new DeterministicLimitValueReference(
                                    limit.getId(), LimitValueTypeEnum.DETERMINISTIC_ATTENTION));
                        }
                        if (limit.getAlertValue() != null) {
                            detLimits.add(new DeterministicLimitValueReference(
                                    limit.getId(), LimitValueTypeEnum.DETERMINISTIC_ALERT));
                        }
                        if (limit.getEmergencyValue() != null) {
                            detLimits.add(new DeterministicLimitValueReference(
                                    limit.getId(), LimitValueTypeEnum.DETERMINISTIC_EMERGENCY));
                        }
                    }
                }
            }

            for (ConstantEntity constant : instrument.getConstants()) {
                constantIds.add(constant.getId());
            }

            UpdateGraphPropertiesRequestDTO request = new UpdateGraphPropertiesRequestDTO();
            request.setInstrumentIds(Collections.emptyList());
            request.setOutputIds(outputIds);
            request.setConstantIds(constantIds);
            request.setStatisticalLimitValues(statLimits);
            request.setDeterministicLimitValues(detLimits);

            propertiesService.updateProperties(patternId, request);

            log.info("Configuradas {} outputs, {} constantes, {} limites estatísticos e {} limites determinísticos para o padrão {} do instrumento {}",
                    outputIds.size(), constantIds.size(), statLimits.size(), detLimits.size(), patternId, instrument.getId());

        } catch (Exception e) {
            log.error("Erro ao configurar propriedades do padrão de gráfico {}: {}",
                    patternId, e.getMessage(), e);
        }
    }

    private void createTabulatePatternForInstrument(InstrumentEntity instrument) {
        String patternName = "Padrão Automático - " + instrument.getName();

        CreateTabulatePatternRequestDTO request = new CreateTabulatePatternRequestDTO();
        request.setName(patternName);
        request.setDamId(instrument.getDam().getId());
        request.setFolderId(null);

        List<CreateTabulatePatternRequestDTO.InstrumentAssociationDTO> associations = new ArrayList<>();
        CreateTabulatePatternRequestDTO.InstrumentAssociationDTO association
                = new CreateTabulatePatternRequestDTO.InstrumentAssociationDTO();

        association.setInstrumentId(instrument.getId());
        association.setIsDateEnable(true);
        association.setDateIndex(1);
        association.setIsHourEnable(true);
        association.setHourIndex(2);
        association.setIsUserEnable(true);
        association.setUserIndex(3);
        association.setIsReadEnable(true);

        List<CreateTabulatePatternRequestDTO.OutputAssociationDTO> outputAssociations = new ArrayList<>();
        int index = 4;

        for (OutputEntity output : instrument.getOutputs()) {
            if (Boolean.TRUE.equals(output.getActive())) {
                CreateTabulatePatternRequestDTO.OutputAssociationDTO outputAssoc
                        = new CreateTabulatePatternRequestDTO.OutputAssociationDTO();

                outputAssoc.setOutputId(output.getId());
                outputAssoc.setOutputIndex(index++);

                outputAssociations.add(outputAssoc);
            }
        }

        if (outputAssociations.isEmpty()) {
            log.warn("Não foi possível criar padrão de tabela para o instrumento {} porque não há outputs ativos",
                    instrument.getId());
            return;
        }

        association.setOutputAssociations(outputAssociations);
        associations.add(association);
        request.setAssociations(associations);

        try {
            tabulatePatternService.create(request);
            log.debug("Padrão de tabela criado para o instrumento: {}", instrument.getId());
        } catch (Exception e) {
            log.error("Erro ao criar padrão de tabela para o instrumento {}: {}",
                    instrument.getId(), e.getMessage(), e);
        }
    }
}
