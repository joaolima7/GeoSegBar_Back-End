package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;
import com.geosegbar.exceptions.NotFoundException;

public class FindByIdDamUseCase {
    
    private final DamRepositoryAdapter damRepositoryAdapter;

    public FindByIdDamUseCase(DamRepositoryAdapter damRepositoryAdapter) {
        this.damRepositoryAdapter = damRepositoryAdapter;
    }

    public DamEntity findById(Long id) {
        return damRepositoryAdapter.findById(id).
        orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));
    }
}
