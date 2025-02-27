package com.geosegbar.infra.sex.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.infra.sex.handler.SexHandler;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SexJpaRepositoryImp implements SexRepositoryAdapter {

    private final SexRepository sexRepository;

    @Autowired
    private SexHandler sexHandler;

    @Override
    public void deleteById(Long id) {
        sexRepository.deleteById(id);
    }

    @Override
    public SexEntity save(SexEntity sexEntity) {
        SexModel sexModel = sexRepository.save(sexHandler.fromEntity(sexEntity).toModel());
        return sexHandler.fromModel(sexModel).toEntity();
    }

    @Override
    public SexEntity update(SexEntity sexEntity) {
        SexModel sexModel = sexRepository.save(sexHandler.fromEntity(sexEntity).toModel());
        return sexHandler.fromModel(sexModel).toEntity();
    }

    @Override
    public Optional<SexEntity> findById(Long id) {
        Optional<SexModel> sexModel = sexRepository.findById(id);
        return sexModel.map(model -> sexHandler.fromModel(model).toEntity());
    }

    @Override
    public List<SexEntity> findAll() {
        List<SexModel> sexModels = sexRepository.findAllByOrderByIdAsc();
        return sexModels.stream().map(sexModel -> sexHandler.fromModel(sexModel).toEntity()).toList();
    }
    
}
