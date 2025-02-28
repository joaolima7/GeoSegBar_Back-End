package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.core.dam.entities.DamEntity;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindByIdDamUseCase {
    
    private final DamRepositoryAdapter damRepositoryAdapter;

    public DamEntity findById(Long id) {
        return damRepositoryAdapter.findById(id).
        orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));
    }
}
