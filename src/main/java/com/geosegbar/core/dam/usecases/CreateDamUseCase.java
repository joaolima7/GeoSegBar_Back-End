package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateDamUseCase {
    private final DamRepositoryAdapter damRepositoryAdapter;

    public DamEntity create(DamEntity damEntity) {
        return damRepositoryAdapter.save(damEntity);
    }
}
