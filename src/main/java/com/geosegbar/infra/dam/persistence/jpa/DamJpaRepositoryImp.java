package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DamJpaRepositoryImp implements DamRepositoryAdapter{
    
    private final DamRepository damRepository;

    @Override
    public void deleteById(Long id) {
        damRepository.deleteById(id);
    }

    @Override
    public DamEntity save(DamEntity damEntity) {
        return damRepository.save(damEntity);
    }

    @Override
    public DamEntity update(DamEntity damEntity) {
        return damRepository.save(damEntity);
    }

    @Override
    public Optional<DamEntity> findById(Long id) {
        return damRepository.findById(id);
    }

    @Override
    public List<DamEntity> findAll() {
        return damRepository.findAllByOrderByIdAsc();
    }

    @Override
    public boolean existsByName(String name) {
        return damRepository.existsByName(name);
    }

    @Override
    public boolean existsByAcronym(String acronym) {
        return damRepository.existsByAcronym(acronym);
    }
}
