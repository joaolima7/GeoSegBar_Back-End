package com.geosegbar.infra.deterministic_limit.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.deterministic_limit.persistence.jpa.DeterministicLimitRepository;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeterministicLimitService {

    private final DeterministicLimitRepository deterministicLimitRepository;
    private final InstrumentRepository instrumentRepository;

    public Optional<DeterministicLimitEntity> findByInstrumentId(Long instrumentId) {
        return deterministicLimitRepository.findByInstrumentId(instrumentId);
    }

    public DeterministicLimitEntity findById(Long id) {
        return deterministicLimitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Limite determinístico não encontrado com ID: " + id));
    }

    @Transactional
    public DeterministicLimitEntity createOrUpdate(Long instrumentId, DeterministicLimitEntity limit) {
        InstrumentEntity instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

        if (Boolean.TRUE.equals(instrument.getNoLimit())) {
            throw new IllegalStateException("Não é possível adicionar limites a um instrumento marcado como 'Sem Limites'");
        }

        if (instrument.getStatisticalLimit() != null) {
            throw new IllegalStateException("Este instrumento já possui limite estatístico. Não é possível ter ambos os tipos de limite");
        }

        Optional<DeterministicLimitEntity> existingLimit = deterministicLimitRepository.findByInstrumentId(instrumentId);

        if (existingLimit.isPresent()) {
            DeterministicLimitEntity existingEntity = existingLimit.get();
            existingEntity.setAttentionValue(limit.getAttentionValue());
            existingEntity.setAlertValue(limit.getAlertValue());
            existingEntity.setEmergencyValue(limit.getEmergencyValue());
            return deterministicLimitRepository.save(existingEntity);
        } else {
            limit.setInstrument(instrument);
            DeterministicLimitEntity savedLimit = deterministicLimitRepository.save(limit);
            instrument.setDeterministicLimit(savedLimit);
            instrumentRepository.save(instrument);
            return savedLimit;
        }
    }

    @Transactional
    public void deleteById(Long id) {
        DeterministicLimitEntity limit = findById(id);
        InstrumentEntity instrument = limit.getInstrument();

        if (instrument != null) {
            instrument.setDeterministicLimit(null);
            instrumentRepository.save(instrument);
        }

        deterministicLimitRepository.delete(limit);
        log.info("Limite determinístico excluído para o instrumento ID: {}",
                instrument != null ? instrument.getId() : "desconhecido");
    }
}
