package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;
import com.geosegbar.infra.dam.handler.DamHandler;

@Component
public class DamJpaRepositoryImp implements DamRepositoryAdapter{
    
    private final DamRepository damRepository;

    @Autowired
    private DamHandler damHandler;

    public DamJpaRepositoryImp(DamRepository damRepository) {
        this.damRepository = damRepository;
    }

    @Override
    public void deleteById(Long id) {
        damRepository.deleteById(id);
    }

    @Override
    public DamEntity save(DamEntity damEntity) {
        DamModel damModel = damRepository.save(damHandler.fromEntity(damEntity).toModel());
        return damHandler.fromModel(damModel).toEntity();
    }

    @Override
    public DamEntity update(DamEntity damEntity) {
        DamModel damModel = damRepository.save(damHandler.fromEntity(damEntity).toModel());
        return damHandler.fromModel(damModel).toEntity();
    }

    @Override
    public Optional<DamEntity> findById(Long id) {
        Optional<DamModel> damModel = damRepository.findById(id);
        return damModel.map(model -> damHandler.fromModel(model).toEntity());
    }

    @Override
    public List<DamEntity> findAll() {
        List<DamModel> damModels = damRepository.findAllByOrderByIdAsc();
        return damModels.stream().map(damModel -> damHandler.fromModel(damModel).toEntity()).toList();
    }
}
