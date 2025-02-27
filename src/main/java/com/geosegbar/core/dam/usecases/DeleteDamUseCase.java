package com.geosegbar.core.dam.usecases;

import com.geosegbar.adapters.dam.DamRepositoryAdapter;
import com.geosegbar.exceptions.NotFoundException;

public class DeleteDamUseCase {
    
    private final DamRepositoryAdapter damRepositoryAdapter;

    public DeleteDamUseCase(DamRepositoryAdapter damRepositoryAdapter) {
        this.damRepositoryAdapter = damRepositoryAdapter;
    }

    public void delete(Long id) {
        damRepositoryAdapter.findById(id)
        .orElseThrow(() -> new NotFoundException("Barragem não encontrada para exclusão!"));
        damRepositoryAdapter.deleteById(id);
    }
}
