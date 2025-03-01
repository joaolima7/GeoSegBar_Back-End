package com.geosegbar.infra.sex.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.sex.persistence.jpa.SexRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SexService {

    private final SexRepository sexRepository;

    @Transactional
    public void deleteById(Long id) {
        sexRepository.findById(id).
        orElseThrow(() -> new NotFoundException("Sexo não encontrado para exclusão!"));

        sexRepository.deleteById(id);
    }

    @Transactional
    public SexEntity save(SexEntity sexEntity) {
        if (sexRepository.existsByName(sexEntity.getName())) {
            throw new DuplicateResourceException("Já existe um sexo com este nome!");
        }
        return sexRepository.save(sexEntity);
    }

    @Transactional
    public SexEntity update(SexEntity sexEntity) {
        sexRepository.findById(sexEntity.getId()).
        orElseThrow(() -> new NotFoundException("Sexo não encontrado para atualização!"));

        if(sexRepository.existsByNameAndIdNot(sexEntity.getName(), sexEntity.getId())){
            throw new DuplicateResourceException("Já existe um sexo com este nome!");
        }

        return sexRepository.save(sexEntity);
    }

    public SexEntity findById(Long id) {
        return sexRepository.findById(id).
        orElseThrow(() -> new NotFoundException("Sexo não encontrado!"));
    }

    public List<SexEntity> findAll() {
        return sexRepository.findAllByOrderByIdAsc();
    }

    public boolean existsByName(String name) {
        return sexRepository.existsByName(name);
    }

    public boolean existsByNameAndIdNot(String name, Long id) {
        return sexRepository.existsByNameAndIdNot(name, id);
    } 

}
