package com.geosegbar.infra.instrument.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.infra.instrument_graph_pattern.dtos.CreateGraphPatternRequest;
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

    @Async
    @Transactional
    public void createPatternsForInstrument(InstrumentEntity instrument) {
        // Verificar se o instrumento não é uma régua linimétrica
        if (Boolean.TRUE.equals(instrument.getIsLinimetricRuler())) {
            log.debug("Ignorando criação de padrões para régua linimétrica: {}", instrument.getId());
            return;
        }

        try {
            // Criar padrão de gráfico
            createGraphPatternForInstrument(instrument);

            // Criar padrão de tabela
            createTabulatePatternForInstrument(instrument);

            log.info("Padrões de gráfico e tabela criados com sucesso para o instrumento: {}", instrument.getId());
        } catch (Exception e) {
            log.error("Erro ao criar padrões automáticos para o instrumento {}: {}",
                    instrument.getId(), e.getMessage(), e);
        }
    }

    private void createGraphPatternForInstrument(InstrumentEntity instrument) {
        String patternName = "Padrão Automático - " + instrument.getName();

        CreateGraphPatternRequest request = new CreateGraphPatternRequest();
        request.setInstrumentId(instrument.getId());
        request.setName(patternName);
        request.setFolderId(null); // Sem pasta

        try {
            graphPatternService.create(request);
            log.debug("Padrão de gráfico criado para o instrumento: {}", instrument.getId());
        } catch (Exception e) {
            log.error("Erro ao criar padrão de gráfico para o instrumento {}: {}",
                    instrument.getId(), e.getMessage(), e);
        }
    }

    private void createTabulatePatternForInstrument(InstrumentEntity instrument) {
        String patternName = "Padrão Automático - " + instrument.getName();

        CreateTabulatePatternRequestDTO request = new CreateTabulatePatternRequestDTO();
        request.setName(patternName);
        request.setDamId(instrument.getDam().getId());
        request.setFolderId(null); // Sem pasta

        // Adicionar todos os outputs ativos do instrumento
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

        // Adicionar todos os outputs do instrumento
        List<CreateTabulatePatternRequestDTO.OutputAssociationDTO> outputAssociations = new ArrayList<>();
        int index = 4; // Começar após data, hora e usuário

        for (OutputEntity output : instrument.getOutputs()) {
            if (Boolean.TRUE.equals(output.getActive())) {
                CreateTabulatePatternRequestDTO.OutputAssociationDTO outputAssoc
                        = new CreateTabulatePatternRequestDTO.OutputAssociationDTO();

                outputAssoc.setOutputId(output.getId());
                outputAssoc.setOutputIndex(index++);

                outputAssociations.add(outputAssoc);
            }
        }

        // Se não houver outputs, não podemos criar o padrão de tabela
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
