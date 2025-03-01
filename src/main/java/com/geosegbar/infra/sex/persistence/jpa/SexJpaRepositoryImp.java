package com.geosegbar.infra.sex.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Qualifier("sexRepository")
public class SexJpaRepositoryImp implements SexRepositoryAdapter{

    private final SexRepository sexRepository;

    @Override
    public void deleteById(Long id) {
        sexRepository.deleteById(id);
    }

    @Override
    public SexEntity save(SexEntity sexEntity) {
        return sexRepository.save(sexEntity);
    }

    @Override
    public SexEntity update(SexEntity sexEntity) {
        return sexRepository.save(sexEntity);
    }

    @Override
    public Optional<SexEntity> findById(Long id) {
        return sexRepository.findById(id);
    }

    @Override
    public List<SexEntity> findAll() {
        return sexRepository.findAllByOrderByIdAsc();
    }

    @Override
    public boolean existsByName(String name) {
        return sexRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        return sexRepository.existsByNameAndIdNot(name, id);
    } 
}
