package com.geosegbar.infra.instrument_type.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.instrument_type.dtos.InstrumentTypeDTO;
import com.geosegbar.infra.instrument_type.persistence.jpa.InstrumentTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstrumentTypeService {

    private final InstrumentTypeRepository instrumentTypeRepository;

    @Transactional(readOnly = true)
    public List<InstrumentTypeDTO> findAll() {
        return instrumentTypeRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstrumentTypeDTO findById(Long id) {
        return mapToDTO(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public InstrumentTypeEntity getEntityById(Long id) {
        return instrumentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de instrumento não encontrado com ID: " + id));
    }

    @Transactional
    public InstrumentTypeDTO create(InstrumentTypeDTO dto) {
        if (instrumentTypeRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Já existe um tipo de instrumento com o nome: " + dto.getName());
        }

        InstrumentTypeEntity entity = new InstrumentTypeEntity();
        entity.setName(dto.getName().trim().toUpperCase());

        return mapToDTO(instrumentTypeRepository.save(entity));
    }

    @Transactional
    public InstrumentTypeDTO update(Long id, InstrumentTypeDTO dto) {
        InstrumentTypeEntity entity = getEntityById(id);

        if (instrumentTypeRepository.existsByName(dto.getName())
                && !entity.getName().equalsIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Já existe um tipo de instrumento com o nome: " + dto.getName());
        }

        entity.setName(dto.getName().trim().toUpperCase());

        return mapToDTO(instrumentTypeRepository.save(entity));
    }

    private InstrumentTypeDTO mapToDTO(InstrumentTypeEntity entity) {
        InstrumentTypeDTO dto = new InstrumentTypeDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }
}
