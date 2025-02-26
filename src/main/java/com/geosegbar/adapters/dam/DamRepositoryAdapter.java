package com.geosegbar.adapters.dam;

import java.util.List;
import java.util.Optional;

import com.geosegbar.core.dam.entities.DamEntity;

public interface DamRepositoryAdapter {
    void deleteById(Long id);
    DamEntity save(DamEntity DamEntity);
    DamEntity update(DamEntity DamEntity);
    Optional<DamEntity> findById(Long id);
    List<DamEntity> findAll();
}
