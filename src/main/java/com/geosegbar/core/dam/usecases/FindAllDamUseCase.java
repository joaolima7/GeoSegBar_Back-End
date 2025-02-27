package com.geosegbar.core.dam.usecases;

import java.util.List;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;

public class FindAllDamUseCase {
    
    private final DamRepositoryAdapter damRepositoryAdapter;

    public FindAllDamUseCase(DamRepositoryAdapter damRepositoryAdapter) {
        this.damRepositoryAdapter = damRepositoryAdapter;
    }

    public List<DamEntity> findAll() {
        return damRepositoryAdapter.findAll();
    }
}
