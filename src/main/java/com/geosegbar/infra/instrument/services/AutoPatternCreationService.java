package com.geosegbar.infra.instrument.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.infra.audit.services.AuditContext;
import com.geosegbar.infra.audit.services.AuditService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
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

    private static final String ACTION = "AUTO_PATTERN_CREATION";
    private static final String ACTION_LABEL = "Criação automática de padrões do instrumento";

    private final InstrumentGraphPatternService graphPatternService;
    private final InstrumentTabulatePatternService tabulatePatternService;
    private final InstrumentGraphCustomizationPropertiesService propertiesService;
    private final InstrumentRepository instrumentRepository;
    private final AuditService auditService;

    /**
     * Cria os padrões automáticos (gráfico e tabela) de um instrumento.
     * <p>
     * <b>Este método NÃO é transacional de propósito.</b> Cada etapa abre a sua
     * própria transação (os {@code create}/{@code updateProperties} dos services
     * já são {@code @Transactional}), de modo que uma falha em uma etapa jamais
     * desfaz o que as anteriores já persistiram.
     * <p>
     * Antes, este método era {@code @Transactional} e todas as etapas entravam na
     * MESMA transação (propagação {@code REQUIRED}). Quando a criação do padrão de
     * tabela falhava (ex.: nome duplicado na barragem), o Spring marcava a
     * transação como {@code rollback-only}; o {@code catch} engolia a exceção e o
     * fluxo seguia logando/auditando "sucesso", mas no commit vinha
     * {@code UnexpectedRollbackException} e <b>o padrão de gráfico já criado era
     * revertido junto</b> — perdendo silenciosamente os dois padrões.
     */
    @Async
    public void createPatternsForInstrument(InstrumentEntity detachedInstrument) {

        Long instrumentId = detachedInstrument.getId();
        long start = System.nanoTime();
        String traceId = auditService.newTraceId();

        // Recarrega o instrumento. O evento é disparado AFTER_COMMIT e processado
        // de forma assíncrona, então a entidade recebida está detached.
        // Usa-se o fetch que traz outputs + limites + CONSTANTES + dam de uma vez:
        // como não há transação/sessão aberta aqui, tudo que for acessado adiante
        // precisa vir carregado (senão: LazyInitializationException).
        InstrumentEntity instrument = instrumentRepository
                .findWithActiveOutputsByIdIn(List.of(instrumentId))
                .stream().findFirst().orElse(null);

        if (instrument == null) {
            log.error("Instrumento {} não encontrado ao criar padrões automáticos. Padrões não criados.", instrumentId);
            auditService.record(AuditContext.builder()
                    .action(ACTION).actionLabel(ACTION_LABEL).source(AuditSource.ASYNC)
                    .status(AuditStatus.ERROR)
                    .message("Instrumento não encontrado ao criar padrões automáticos.")
                    .entityType("Instrument").entityId(instrumentId)
                    .traceId(traceId).durationMs(durationMs(start))
                    .build());
            return;
        }

        if (Boolean.TRUE.equals(instrument.getIsLinimetricRuler())) {
            log.debug("Ignorando criação de padrões para régua linimétrica: {}", instrument.getId());
            return;
        }

        // Cada etapa é independente: o resultado real é rastreado para que o log e
        // a auditoria NUNCA reportem sucesso quando algo falhou (era o que
        // mascarava o problema — 200 auditorias SUCCESS e nenhum padrão criado).
        List<String> falhas = new ArrayList<>();

        GraphPatternResponseDTO graphPattern = null;
        try {
            graphPattern = graphPatternService.create(buildGraphPatternRequest(instrument));
            log.debug("Padrão de gráfico criado para o instrumento: {}", instrument.getId());
        } catch (Exception e) {
            log.error("Erro ao criar padrão de gráfico para o instrumento {}: {}",
                    instrument.getId(), e.getMessage(), e);
            falhas.add("padrão de gráfico: " + e.getMessage());
        }

        // Falha ao configurar propriedades não invalida o padrão já criado — ele
        // permanece (sem customização) em vez de ser perdido.
        if (graphPattern != null) {
            try {
                configureGraphPatternProperties(instrument, graphPattern.getId());
                log.info("Propriedades do padrão de gráfico configuradas automaticamente para o instrumento: {}", instrument.getId());
            } catch (Exception e) {
                log.error("Erro ao configurar propriedades do padrão de gráfico {} do instrumento {}: {}",
                        graphPattern.getId(), instrument.getId(), e.getMessage(), e);
                falhas.add("propriedades do gráfico: " + e.getMessage());
            }
        }

        try {
            createTabulatePatternForInstrument(instrument);
        } catch (Exception e) {
            log.error("Erro ao criar padrão de tabela para o instrumento {}: {}",
                    instrument.getId(), e.getMessage(), e);
            falhas.add("padrão de tabela: " + e.getMessage());
        }

        if (falhas.isEmpty()) {
            log.info("Padrões de gráfico e tabela criados com sucesso para o instrumento: {}", instrument.getId());
            auditService.record(AuditContext.builder()
                    .action(ACTION).actionLabel(ACTION_LABEL).source(AuditSource.ASYNC)
                    .status(AuditStatus.SUCCESS)
                    .message("Padrões de gráfico e tabela criados para o instrumento " + instrument.getName() + ".")
                    .entityType("Instrument").entityId(instrument.getId())
                    .traceId(traceId).durationMs(durationMs(start))
                    .build());
        } else {
            String detalhe = String.join(" | ", falhas);
            log.error("Padrões automáticos do instrumento {} concluídos COM FALHAS: {}", instrument.getId(), detalhe);
            auditService.record(AuditContext.builder()
                    .action(ACTION).actionLabel(ACTION_LABEL).source(AuditSource.ASYNC)
                    .status(AuditStatus.ERROR)
                    .message("Falha ao criar padrões automáticos para o instrumento "
                            + instrument.getName() + " (gráfico criado: " + (graphPattern != null) + "). " + detalhe)
                    .entityType("Instrument").entityId(instrument.getId())
                    .traceId(traceId).durationMs(durationMs(start))
                    .build());
        }
    }

    private CreateGraphPatternRequest buildGraphPatternRequest(InstrumentEntity instrument) {
        CreateGraphPatternRequest request = new CreateGraphPatternRequest();
        request.setInstrumentId(instrument.getId());
        request.setName("Padrão Automático - " + instrument.getName());
        request.setFolderId(null);
        return request;
    }

    private long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /**
     * Cria apenas o padrão de gráfico (usado pela rotina de reparo).
     * <p>
     * NÃO é transacional: a criação do padrão e a configuração das propriedades
     * abrem transações próprias. Assim, se a configuração das propriedades
     * falhar, o padrão criado <b>permanece</b> em vez de ser revertido.
     * A exceção da criação é propagada para o chamador decidir o que fazer.
     */
    public void createGraphPatternOnly(InstrumentEntity instrument) {
        GraphPatternResponseDTO graphPattern = graphPatternService.create(buildGraphPatternRequest(instrument));

        try {
            configureGraphPatternProperties(instrument, graphPattern.getId());
        } catch (Exception e) {
            log.error("Padrão de gráfico {} criado, mas falhou ao configurar propriedades do instrumento {}: {}",
                    graphPattern.getId(), instrument.getId(), e.getMessage(), e);
        }
    }

    /**
     * Cria apenas o padrão de tabela (usado pela rotina de reparo). NÃO é
     * transacional — o {@code create} do service abre a própria transação, então
     * uma falha aqui não contamina nada que já tenha sido persistido.
     */
    public void createTabulatePatternOnly(InstrumentEntity instrument) {
        createTabulatePatternForInstrument(instrument);
    }

    private void configureGraphPatternProperties(InstrumentEntity instrument, Long patternId) {

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

        // A exceção é propagada de propósito: quem chama decide como tratar e
        // registra o resultado real. Engolir aqui era o que fazia o fluxo seguir
        // reportando "sucesso" mesmo com a criação tendo falhado.
        tabulatePatternService.create(request);
        log.debug("Padrão de tabela criado para o instrumento: {}", instrument.getId());
    }
}
