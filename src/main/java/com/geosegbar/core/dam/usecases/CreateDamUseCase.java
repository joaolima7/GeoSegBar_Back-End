package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;
import com.geosegbar.exceptions.DuplicateResourceException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateDamUseCase {
    private final DamRepositoryAdapter damRepositoryAdapter;

    public DamEntity create(DamEntity damEntity) {
        
        if (damRepositoryAdapter.existsByName(damEntity.getName())) {
            throw new DuplicateResourceException("Já existe uma barragem com este nome!");
        }
        
        if (damRepositoryAdapter.existsByAcronym(damEntity.getAcronym())) {
            throw new DuplicateResourceException("Já existe uma barragem com esta sigla!");
        }

        return damRepositoryAdapter.save(damEntity);
    }
}
