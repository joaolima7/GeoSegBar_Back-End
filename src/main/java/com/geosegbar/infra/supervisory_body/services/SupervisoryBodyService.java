package com.geosegbar.infra.supervisory_body.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.SupervisoryBodyEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.supervisory_body.persistence.SupervisoryBodyRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupervisoryBodyService {
    
    private final SupervisoryBodyRepository supervisoryBodyRepository;

    @Transactional
    public void deleteById(Long id) {
        supervisoryBodyRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Órgão fiscalizador não encontrado para exclusão!"));

        supervisoryBodyRepository.deleteById(id);
    }

    @Transactional
    public SupervisoryBodyEntity save(SupervisoryBodyEntity supervisoryBodyEntity) {
        if (supervisoryBodyRepository.existsByName(supervisoryBodyEntity.getName())) {
            throw new DuplicateResourceException("Já existe um órgão fiscalizador com este nome!");
        }

        return supervisoryBodyRepository.save(supervisoryBodyEntity);
    }

    @Transactional
    public SupervisoryBodyEntity update(SupervisoryBodyEntity supervisoryBodyEntity) {
        supervisoryBodyRepository.findById(supervisoryBodyEntity.getId())
        .orElseThrow(() -> new NotFoundException("Órgão fiscalizador não encontrado para atualização!"));

        if (supervisoryBodyRepository.existsByNameAndIdNot(supervisoryBodyEntity.getName(), supervisoryBodyEntity.getId())) {
            throw new DuplicateResourceException("Já existe um órgão fiscalizador com este nome!");
        }
        
        return supervisoryBodyRepository.save(supervisoryBodyEntity);
    }

    public SupervisoryBodyEntity findById(Long id) {
        return supervisoryBodyRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Órgão fiscalizador não encontrado!"));
    }

    public List<SupervisoryBodyEntity> findAll() {
        return supervisoryBodyRepository.findAllByOrderByIdAsc();
    }
}