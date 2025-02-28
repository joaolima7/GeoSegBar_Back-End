package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateDamUseCase {
    
    private final DamRepositoryAdapter damRepositoryAdapter;

    public DamEntity update(DamEntity damEntity) {
        damRepositoryAdapter.findById(damEntity.getId()).
        orElseThrow(() -> new NotFoundException("Endereço não encontrado para atualização!"));
        return damRepositoryAdapter.update(damEntity);
    }
}
