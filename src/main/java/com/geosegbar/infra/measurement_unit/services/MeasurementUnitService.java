package com.geosegbar.infra.measurement_unit.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.exceptions.BusinessRuleException;
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
            createDefaultUnit("Metros", "M");
            createDefaultUnit("Centímetros", "CM");
            createDefaultUnit("Milímetros", "MM");
            createDefaultUnit("Metros cúbicos", "M³");
            createDefaultUnit("Metros cúbicos por segundo", "M³/S");
            createDefaultUnit("Litros por segundo", "L/S");
            createDefaultUnit("Quilopascal", "KPA");
            createDefaultUnit("Megapascal", "MPA");
            createDefaultUnit("Graus Celsius", "°C");
            createDefaultUnit("Percentual", "%");
            createDefaultUnit("Metros por segundo", "M/S");
            createDefaultUnit("Graus", "°");
        }
    }

    private void createDefaultUnit(String name, String acronym) {
        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setName(name);
        unit.setAcronym(acronym);
        measurementUnitRepository.save(unit);
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

        measurementUnit.setName(formatName(measurementUnit.getName()));
        measurementUnit.setAcronym(formatAcronym(measurementUnit.getAcronym()));

        if (measurementUnitRepository.existsByName(measurementUnit.getName())) {
            throw new DuplicateResourceException("Unidade de medida com nome '" + measurementUnit.getName() + "' já existe");
        }

        if (measurementUnitRepository.existsByAcronym(measurementUnit.getAcronym())) {
            throw new DuplicateResourceException("Unidade de medida com sigla '" + measurementUnit.getAcronym() + "' já existe");
        }

        MeasurementUnitEntity savedUnit = measurementUnitRepository.save(measurementUnit);
        return savedUnit;
    }

    @Transactional
    public MeasurementUnitEntity update(Long id, MeasurementUnitEntity measurementUnit) {
        MeasurementUnitEntity existingUnit = findById(id);

        String normalizedName = formatName(measurementUnit.getName());
        String normalizedAcronym = formatAcronym(measurementUnit.getAcronym());

        if (measurementUnitRepository.existsByNameAndIdNot(normalizedName, id)) {
            throw new DuplicateResourceException("Unidade de medida com nome '" + normalizedName + "' já existe");
        }

        if (measurementUnitRepository.existsByAcronymAndIdNot(normalizedAcronym, id)) {
            throw new DuplicateResourceException("Unidade de medida com sigla '" + normalizedAcronym + "' já existe");
        }

        existingUnit.setName(normalizedName);
        existingUnit.setAcronym(normalizedAcronym);

        MeasurementUnitEntity updatedUnit = measurementUnitRepository.save(existingUnit);
        return updatedUnit;
    }

    @Transactional
    public void delete(Long id) {
        MeasurementUnitEntity measurementUnit = findById(id);
        if (!measurementUnit.getConstants().isEmpty() || !measurementUnit.getOutputs().isEmpty() || !measurementUnit.getInputs().isEmpty()) {
            throw new BusinessRuleException("Não é possível excluir a unidade de medida pois existem registros associados a ela.");
        }

        measurementUnitRepository.delete(measurementUnit);
    }

    private String formatName(String name) {
        return name == null ? null : name.trim();
    }

    private String formatAcronym(String acronym) {
        return acronym == null ? null : acronym.trim();
    }
}
