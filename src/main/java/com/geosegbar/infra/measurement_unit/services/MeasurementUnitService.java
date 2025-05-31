package com.geosegbar.infra.measurement_unit.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.measurement_unit.persistence.jpa.MeasurementUnitRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeasurementUnitService {

    private final MeasurementUnitRepository measurementUnitRepository;

    @PostConstruct
    public void initDefaultUnits() {
        if (measurementUnitRepository.count() == 0) {
            log.info("Inicializando unidades de medida padrão...");

            createDefaultUnit("Metros", "m");
            createDefaultUnit("Centímetros", "cm");
            createDefaultUnit("Milímetros", "mm");
            createDefaultUnit("Metros cúbicos", "m³");
            createDefaultUnit("Metros cúbicos por segundo", "m³/s");
            createDefaultUnit("Litros por segundo", "L/s");
            createDefaultUnit("Quilopascal", "kPa");
            createDefaultUnit("Megapascal", "MPa");
            createDefaultUnit("Graus Celsius", "°C");
            createDefaultUnit("Percentual", "%");
            createDefaultUnit("Metros por segundo", "m/s");
            createDefaultUnit("Graus", "°");

            log.info("Unidades de medida padrão inicializadas com sucesso.");
        }
    }

    private void createDefaultUnit(String name, String acronym) {
        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setName(name);
        unit.setAcronym(acronym);
        measurementUnitRepository.save(unit);
        log.debug("Unidade de medida criada: {} ({})", name, acronym);
    }

    public List<MeasurementUnitEntity> findAll() {
        return measurementUnitRepository.findAllByOrderByNameAsc();
    }

    public MeasurementUnitEntity findById(Long id) {
        return measurementUnitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + id));
    }

    public Optional<MeasurementUnitEntity> findByName(String name) {
        return measurementUnitRepository.findByName(name);
    }

    public Optional<MeasurementUnitEntity> findByAcronym(String acronym) {
        return measurementUnitRepository.findByAcronym(acronym);
    }

    @Transactional
    public MeasurementUnitEntity create(MeasurementUnitEntity measurementUnit) {
        if (measurementUnitRepository.existsByName(measurementUnit.getName())) {
            throw new DuplicateResourceException("Unidade de medida com nome '" + measurementUnit.getName() + "' já existe");
        }

        if (measurementUnitRepository.existsByAcronym(measurementUnit.getAcronym())) {
            throw new DuplicateResourceException("Unidade de medida com sigla '" + measurementUnit.getAcronym() + "' já existe");
        }

        MeasurementUnitEntity savedUnit = measurementUnitRepository.save(measurementUnit);
        log.info("Nova unidade de medida criada: {} ({})", savedUnit.getName(), savedUnit.getAcronym());
        return savedUnit;
    }

    @Transactional
    public MeasurementUnitEntity update(Long id, MeasurementUnitEntity measurementUnit) {
        MeasurementUnitEntity existingUnit = findById(id);

        if (measurementUnitRepository.existsByNameAndIdNot(measurementUnit.getName(), id)) {
            throw new DuplicateResourceException("Unidade de medida com nome '" + measurementUnit.getName() + "' já existe");
        }

        if (measurementUnitRepository.existsByAcronymAndIdNot(measurementUnit.getAcronym(), id)) {
            throw new DuplicateResourceException("Unidade de medida com sigla '" + measurementUnit.getAcronym() + "' já existe");
        }

        existingUnit.setName(measurementUnit.getName());
        existingUnit.setAcronym(measurementUnit.getAcronym());

        MeasurementUnitEntity updatedUnit = measurementUnitRepository.save(existingUnit);
        log.info("Unidade de medida atualizada: {} ({})", updatedUnit.getName(), updatedUnit.getAcronym());
        return updatedUnit;
    }

    @Transactional
    public void delete(Long id) {
        MeasurementUnitEntity measurementUnit = findById(id);
        measurementUnitRepository.delete(measurementUnit);
        log.info("Unidade de medida excluída: {} ({})", measurementUnit.getName(), measurementUnit.getAcronym());
    }
}
