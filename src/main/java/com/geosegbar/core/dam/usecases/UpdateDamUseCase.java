package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateDamUseCase {
    
    private final DamRepositoryAdapter damRepositoryAdapter;

    public DamEntity update(DamEntity damEntity) {
        
        damRepositoryAdapter.findById(damEntity.getId()).
        orElseThrow(() -> new NotFoundException("Endereço não encontrado para atualização!"));

        if (damRepositoryAdapter.existsByNameAndIdNot(damEntity.getName(), damEntity.getId())) {
            throw new DuplicateResourceException("Já existe uma barragem com este nome!");
        }
        
        if (damRepositoryAdapter.existsByAcronymAndIdNot(damEntity.getAcronym(), damEntity.getId())) {
            throw new DuplicateResourceException("Já existe uma barragem com esta sigla!");
        }

        return damRepositoryAdapter.update(damEntity);
    }
}
