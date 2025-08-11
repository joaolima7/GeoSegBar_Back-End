package com.geosegbar.infra.statistical_limit.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.output.persistence.jpa.OutputRepository;
import com.geosegbar.infra.statistical_limit.persistence.jpa.StatisticalLimitRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticalLimitService {

    private final StatisticalLimitRepository statisticalLimitRepository;
    private final OutputRepository outputRepository;

    public Optional<StatisticalLimitEntity> findByOutputId(Long outputId) {
        return statisticalLimitRepository.findByOutputId(outputId);
    }

    public List<Long> findStatisticalLimitIdsByOutputInstrumentDamId(Long damId) {
        return statisticalLimitRepository.findLimitIdsByOutputInstrumentDamId(damId);
    }

    public StatisticalLimitEntity findById(Long id) {
        return statisticalLimitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Limite estatístico não encontrado com ID: " + id));
    }

    @Transactional
    public StatisticalLimitEntity createOrUpdate(Long outputId, StatisticalLimitEntity limit) {
        OutputEntity output = outputRepository.findById(outputId)
                .orElseThrow(() -> new NotFoundException("Output não encontrado com ID: " + outputId));

        InstrumentEntity instrument = output.getInstrument();
        if (instrument == null) {
            throw new IllegalStateException("Output sem instrumento associado");
        }

        if (Boolean.TRUE.equals(instrument.getNoLimit())) {
            throw new IllegalStateException("Não é possível adicionar limites a um output de um instrumento marcado como 'Sem Limites'");
        }

        if (output.getDeterministicLimit() != null) {
            throw new IllegalStateException("Este output já possui limite determinístico. Não é possível ter ambos os tipos de limite");
        }

        Optional<StatisticalLimitEntity> existingLimit = statisticalLimitRepository.findByOutputId(outputId);

        if (existingLimit.isPresent()) {
            StatisticalLimitEntity existingEntity = existingLimit.get();
            existingEntity.setLowerValue(limit.getLowerValue());
            existingEntity.setUpperValue(limit.getUpperValue());
            return statisticalLimitRepository.save(existingEntity);
        } else {
            limit.setOutput(output);
            StatisticalLimitEntity savedLimit = statisticalLimitRepository.save(limit);
            output.setStatisticalLimit(savedLimit);
            outputRepository.save(output);
            return savedLimit;
        }
    }

    @Transactional
    public void deleteById(Long id) {
        StatisticalLimitEntity limit = findById(id);
        OutputEntity output = limit.getOutput();

        if (output != null) {
            output.setStatisticalLimit(null);
            outputRepository.save(output);
        }

        statisticalLimitRepository.delete(limit);
        log.info("Limite estatístico excluído para o output ID: {}",
                output != null ? output.getId() : "desconhecido");
    }
}
