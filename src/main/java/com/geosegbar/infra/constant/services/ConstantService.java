package com.geosegbar.infra.constant.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.constant.persistence.jpa.ConstantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConstantService {

    private final ConstantRepository constantRepository;

    @Transactional(readOnly = true)
    public List<ConstantEntity> findByInstrumentId(Long instrumentId) {
        return constantRepository.findByInstrumentId(instrumentId);
    }

    @Transactional(readOnly = true)
    public ConstantEntity findById(Long id) {

        return constantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Constante não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Long> findConstantIdsByInstrumentDamId(Long damId) {
        return constantRepository.findConstantIdsByInstrumentDamId(damId);
    }

    @Transactional
    public ConstantEntity deleteById(Long id) {

        ConstantEntity constant = findById(id);

        constantRepository.delete(constant);

        log.info("Constante deletada: ID {}", id);

        return constant;
    }
}
