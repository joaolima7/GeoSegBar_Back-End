package com.geosegbar.infra.dam.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DamService {
    

    private final DamRepository damRepository;

    @Transactional
    public void deleteById(Long id) {
        damRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Barragem não encontrada para exclusão!"));

        damRepository.deleteById(id);
    }

    @Transactional
    public DamEntity save(DamEntity damEntity) {
        if (damRepository.existsByName(damEntity.getName())) {
            throw new DuplicateResourceException("Já existe uma barragem com este nome!");
        }
                    
        if (damRepository.existsByAcronym(damEntity.getAcronym())) {
            throw new DuplicateResourceException("Já existe uma barragem com esta sigla!");
        }

        return damRepository.save(damEntity);
    }

    @Transactional
    public DamEntity update(DamEntity damEntity) {
        damRepository.findById(damEntity.getId()).
        orElseThrow(() -> new NotFoundException("Endereço não encontrado para atualização!"));

        if (damRepository.existsByNameAndIdNot(damEntity.getName(), damEntity.getId())) {
            throw new DuplicateResourceException("Já existe uma barragem com este nome!");
        }
        
        if (damRepository.existsByAcronymAndIdNot(damEntity.getAcronym(), damEntity.getId())) {
            throw new DuplicateResourceException("Já existe uma barragem com esta sigla!");
        }
        return damRepository.save(damEntity);
    }

    public DamEntity findById(Long id) {
        return damRepository.findById(id).
        orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));
    }

    public List<DamEntity> findAll() {
        return damRepository.findAllByOrderByIdAsc();
    }

    public boolean existsByName(String name) {
        return damRepository.existsByName(name);
    }

    public boolean existsByAcronym(String acronym) {
        return damRepository.existsByAcronym(acronym);
    }

    public boolean existsByNameAndIdNot(String name, Long id) {
        return damRepository.existsByNameAndIdNot(name, id);
    }

    public boolean existsByAcronymAndIdNot(String acronym, Long id) {
        return damRepository.existsByAcronymAndIdNot(acronym, id);
    }
}
