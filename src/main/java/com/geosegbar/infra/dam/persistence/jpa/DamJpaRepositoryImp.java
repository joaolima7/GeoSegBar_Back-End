package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;

@Component
public class DamJpaRepositoryImp implements DamRepositoryAdapter{
    
    private final DamRepository damRepository;


    public DamJpaRepositoryImp(DamRepository damRepository) {
        this.damRepository = damRepository;
    }

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
}
