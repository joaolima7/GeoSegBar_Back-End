package com.geosegbar.adapters.sex;

import java.util.List;
import java.util.Optional;

import com.geosegbar.core.sex.entities.SexEntity;


public interface SexRepositoryAdapter {
    void deleteById(Long id);
    SexEntity save(SexEntity sexEntity);
    SexEntity update(SexEntity sexEntity);
    Optional<SexEntity> findById(Long id);
    List<SexEntity> findAll();
}
