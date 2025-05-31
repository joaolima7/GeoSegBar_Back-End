package com.geosegbar.infra.statistical_limit.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.statistical_limit.persistence.jpa.StatisticalLimitRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticalLimitService {

    private final StatisticalLimitRepository statisticalLimitRepository;
    private final InstrumentRepository instrumentRepository;

    public Optional<StatisticalLimitEntity> findByInstrumentId(Long instrumentId) {
        return statisticalLimitRepository.findByInstrumentId(instrumentId);
    }

    public StatisticalLimitEntity findById(Long id) {
        return statisticalLimitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Limite estatístico não encontrado com ID: " + id));
    }

    @Transactional
    public StatisticalLimitEntity createOrUpdate(Long instrumentId, StatisticalLimitEntity limit) {
        InstrumentEntity instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

        if (Boolean.TRUE.equals(instrument.getNoLimit())) {
            throw new IllegalStateException("Não é possível adicionar limites a um instrumento marcado como 'Sem Limites'");
        }

        if (instrument.getDeterministicLimit() != null) {
            throw new IllegalStateException("Este instrumento já possui limite determinístico. Não é possível ter ambos os tipos de limite");
        }

        Optional<StatisticalLimitEntity> existingLimit = statisticalLimitRepository.findByInstrumentId(instrumentId);

        if (existingLimit.isPresent()) {
            StatisticalLimitEntity existingEntity = existingLimit.get();
            existingEntity.setLowerValue(limit.getLowerValue());
            existingEntity.setUpperValue(limit.getUpperValue());
            return statisticalLimitRepository.save(existingEntity);
        } else {
            limit.setInstrument(instrument);
            StatisticalLimitEntity savedLimit = statisticalLimitRepository.save(limit);
            instrument.setStatisticalLimit(savedLimit);
            instrumentRepository.save(instrument);
            return savedLimit;
        }
    }

    @Transactional
    public void deleteById(Long id) {
        StatisticalLimitEntity limit = findById(id);
        InstrumentEntity instrument = limit.getInstrument();

        if (instrument != null) {
            instrument.setStatisticalLimit(null);
            instrumentRepository.save(instrument);
        }

        statisticalLimitRepository.delete(limit);
        log.info("Limite estatístico excluído para o instrumento ID: {}",
                instrument != null ? instrument.getId() : "desconhecido");
    }
}
