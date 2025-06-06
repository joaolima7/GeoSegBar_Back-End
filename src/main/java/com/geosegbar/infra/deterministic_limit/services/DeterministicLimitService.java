package com.geosegbar.infra.deterministic_limit.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.deterministic_limit.persistence.jpa.DeterministicLimitRepository;
import com.geosegbar.infra.output.persistence.jpa.OutputRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeterministicLimitService {

    private final DeterministicLimitRepository deterministicLimitRepository;
    private final OutputRepository outputRepository;

    public Optional<DeterministicLimitEntity> findByOutputId(Long outputId) {
        return deterministicLimitRepository.findByOutputId(outputId);
    }

    public DeterministicLimitEntity findById(Long id) {
        return deterministicLimitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Limite determinístico não encontrado com ID: " + id));
    }

    @Transactional
    public DeterministicLimitEntity createOrUpdate(Long outputId, DeterministicLimitEntity limit) {
        OutputEntity output = outputRepository.findById(outputId)
                .orElseThrow(() -> new NotFoundException("Output não encontrado com ID: " + outputId));

        InstrumentEntity instrument = output.getInstrument();
        if (instrument == null) {
            throw new IllegalStateException("Output sem instrumento associado");
        }

        if (Boolean.TRUE.equals(instrument.getNoLimit())) {
            throw new IllegalStateException("Não é possível adicionar limites a um output de um instrumento marcado como 'Sem Limites'");
        }

        if (output.getStatisticalLimit() != null) {
            throw new IllegalStateException("Este output já possui limite estatístico. Não é possível ter ambos os tipos de limite");
        }

        Optional<DeterministicLimitEntity> existingLimit = deterministicLimitRepository.findByOutputId(outputId);

        if (existingLimit.isPresent()) {
            DeterministicLimitEntity existingEntity = existingLimit.get();
            existingEntity.setAttentionValue(limit.getAttentionValue());
            existingEntity.setAlertValue(limit.getAlertValue());
            existingEntity.setEmergencyValue(limit.getEmergencyValue());
            return deterministicLimitRepository.save(existingEntity);
        } else {
            limit.setOutput(output);
            DeterministicLimitEntity savedLimit = deterministicLimitRepository.save(limit);
            output.setDeterministicLimit(savedLimit);
            outputRepository.save(output);
            return savedLimit;
        }
    }

    @Transactional
    public void deleteById(Long id) {
        DeterministicLimitEntity limit = findById(id);
        OutputEntity output = limit.getOutput();

        if (output != null) {
            output.setDeterministicLimit(null);
            outputRepository.save(output);
        }

        deterministicLimitRepository.delete(limit);
        log.info("Limite determinístico excluído para o output ID: {}",
                output != null ? output.getId() : "desconhecido");
    }
}
