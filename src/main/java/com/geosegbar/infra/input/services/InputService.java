package com.geosegbar.infra.input.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InputEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.input.persistence.jpa.InputRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InputService {

    private final InputRepository inputRepository;

    @Transactional(readOnly = true)
    public List<InputEntity> findByInstrumentId(Long instrumentId) {
        return inputRepository.findByInstrumentId(instrumentId);
    }

    @Transactional(readOnly = true)
    public InputEntity findById(Long id) {

        return inputRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Input não encontrado com ID: " + id));
    }

    @Transactional
    public InputEntity deleteById(Long id) {

        InputEntity input = findById(id);

        inputRepository.delete(input);

        log.info("Input excluído: {}", input.getName());

        return input;
    }
}
