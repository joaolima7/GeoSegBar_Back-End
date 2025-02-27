package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;

public class CreateDamUseCase {
    private final DamRepositoryAdapter damRepositoryAdapter;

    public CreateDamUseCase(DamRepositoryAdapter damRepositoryAdapter) {
        this.damRepositoryAdapter = damRepositoryAdapter;
    }

    public DamEntity create(DamEntity damEntity) {
        return damRepositoryAdapter.save(damEntity);
    }
}
