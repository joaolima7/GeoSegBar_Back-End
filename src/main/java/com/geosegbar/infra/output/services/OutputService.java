package com.geosegbar.infra.output.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.OutputEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.output.persistence.jpa.OutputRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutputService {

    private final OutputRepository outputRepository;

    public List<OutputEntity> findByInstrumentId(Long instrumentId) {
        return outputRepository.findByInstrumentIdAndActiveTrue(instrumentId);
    }

    public OutputEntity findById(Long id) {
        return outputRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Output não encontrado com ID: " + id));
    }

    @Transactional
    public OutputEntity deleteById(Long id) {
        OutputEntity output = findById(id);

        log.info("Deletando output {}.", id);

        outputRepository.delete(output);

        return output;
    }
}
