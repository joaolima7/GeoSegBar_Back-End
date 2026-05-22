package com.geosegbar.infra.instrument.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@DependsOn("autoPatternCreationService")
public class MissingAutoPatternRepairService {

    private static final int BATCH_SIZE = 20;

    private final InstrumentRepository instrumentRepository;
    private final AutoPatternCreationService autoPatternCreationService;

    @PostConstruct
    public void repairMissingAutoPatterns() {
        try {
            repairMissingGraphPatterns();
            repairMissingTabulatePatterns();
        } catch (Exception e) {
            log.error("[AutoPatternRepair] Erro durante reparo de padrões automáticos: {}", e.getMessage(), e);
        }
    }

    private void repairMissingGraphPatterns() {
        List<Long> ids = instrumentRepository.findActiveNonLinimetricIdsWithoutAutoGraphPattern();
        if (ids.isEmpty()) {
            log.info("[AutoPatternRepair] Nenhum instrumento sem padrão de gráfico automático.");
            return;
        }
        log.warn("[AutoPatternRepair] {} instrumento(s) sem padrão de gráfico automático. Iniciando reparo...", ids.size());

        int repaired = 0;
        for (List<Long> batch : partition(ids, BATCH_SIZE)) {
            repaired += repairGraphPatternBatch(batch);
        }
        log.info("[AutoPatternRepair] Padrões de gráfico criados: {}/{}", repaired, ids.size());
    }

    private void repairMissingTabulatePatterns() {
        List<Long> ids = instrumentRepository.findActiveNonLinimetricIdsWithoutAutoTabulatePattern();
        if (ids.isEmpty()) {
            log.info("[AutoPatternRepair] Nenhum instrumento sem padrão de tabela automático.");
            return;
        }
        log.warn("[AutoPatternRepair] {} instrumento(s) sem padrão de tabela automático. Iniciando reparo...", ids.size());

        int repaired = 0;
        for (List<Long> batch : partition(ids, BATCH_SIZE)) {
            repaired += repairTabulatePatternBatch(batch);
        }
        log.info("[AutoPatternRepair] Padrões de tabela criados: {}/{}", repaired, ids.size());
    }

    @Transactional
    public int repairGraphPatternBatch(List<Long> ids) {
        List<InstrumentEntity> instruments = instrumentRepository.findWithActiveOutputsByIdIn(ids);
        int count = 0;
        for (InstrumentEntity instrument : instruments) {
            try {
                autoPatternCreationService.createGraphPatternOnly(instrument);
                count++;
            } catch (Exception e) {
                log.error("[AutoPatternRepair] Falha ao criar padrão de gráfico para instrumento {}: {}",
                        instrument.getId(), e.getMessage());
            }
        }
        return count;
    }

    @Transactional
    public int repairTabulatePatternBatch(List<Long> ids) {
        List<InstrumentEntity> instruments = instrumentRepository.findWithActiveOutputsByIdIn(ids);
        int count = 0;
        for (InstrumentEntity instrument : instruments) {
            try {
                autoPatternCreationService.createTabulatePatternOnly(instrument);
                count++;
            } catch (Exception e) {
                log.error("[AutoPatternRepair] Falha ao criar padrão de tabela para instrumento {}: {}",
                        instrument.getId(), e.getMessage());
            }
        }
        return count;
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
