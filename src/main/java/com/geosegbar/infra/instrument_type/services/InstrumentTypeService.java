package com.geosegbar.infra.instrument_type.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.instrument_type.persistence.jpa.InstrumentTypeRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentTypeService {

    private final InstrumentTypeRepository instrumentTypeRepository;

    @PostConstruct
    public void initDefaultTypes() {
        if (instrumentTypeRepository.count() == 0) {
            InstrumentTypeEntity piezometer = new InstrumentTypeEntity();
            piezometer.setName("Piezômetro de tubo aberto");
            instrumentTypeRepository.save(piezometer);
        }
    }

    public List<InstrumentTypeEntity> findAll() {
        return instrumentTypeRepository.findAllByOrderByNameAsc();
    }

    public InstrumentTypeEntity findById(Long id) {
        return instrumentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de instrumento não encontrado com ID: " + id));
    }

    public Optional<InstrumentTypeEntity> findByName(String name) {
        return instrumentTypeRepository.findByName(name);
    }

    @Transactional
    public InstrumentTypeEntity create(InstrumentTypeEntity instrumentType) {
        if (instrumentTypeRepository.existsByName(instrumentType.getName())) {
            throw new DuplicateResourceException("Tipo de instrumento com nome '" + instrumentType.getName() + "' já existe");
        }

        InstrumentTypeEntity savedType = instrumentTypeRepository.save(instrumentType);
        return savedType;
    }

    @Transactional
    public InstrumentTypeEntity update(Long id, InstrumentTypeEntity instrumentType) {
        InstrumentTypeEntity existingType = findById(id);

        if (instrumentTypeRepository.existsByNameAndIdNot(instrumentType.getName(), id)) {
            throw new DuplicateResourceException("Tipo de instrumento com nome '" + instrumentType.getName() + "' já existe");
        }

        existingType.setName(instrumentType.getName());

        InstrumentTypeEntity updatedType = instrumentTypeRepository.save(existingType);
        return updatedType;
    }

    @Transactional
    public void delete(Long id) {
        InstrumentTypeEntity instrumentType = findById(id);
        instrumentTypeRepository.delete(instrumentType);
    }
}
