package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;
import com.geosegbar.exceptions.NotFoundException;

public class UpdateDamUseCase {
    
    private final DamRepositoryAdapter damRepositoryAdapter;

    public UpdateDamUseCase(DamRepositoryAdapter damRepositoryAdapter) {
        this.damRepositoryAdapter = damRepositoryAdapter;
    }

    public DamEntity update(DamEntity damEntity) {
        damRepositoryAdapter.findById(damEntity.getId()).
        orElseThrow(() -> new NotFoundException("Endereço não encontrado para atualização!"));
        return damRepositoryAdapter.update(damEntity);
    }
}
