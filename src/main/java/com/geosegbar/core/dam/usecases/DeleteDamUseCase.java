package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteDamUseCase {
    
    private final DamRepositoryAdapter damRepositoryAdapter;

    public void delete(Long id) {
        damRepositoryAdapter.findById(id)
        .orElseThrow(() -> new NotFoundException("Barragem não encontrada para exclusão!"));
        damRepositoryAdapter.deleteById(id);
    }
}
