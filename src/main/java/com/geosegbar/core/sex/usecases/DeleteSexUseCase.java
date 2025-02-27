package com.geosegbar.core.sex.usecases;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.exceptions.NotFoundException;

public class DeleteSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public DeleteSexUseCase(SexRepositoryAdapter sexRepositoryAdapter) {
        this.sexRepositoryAdapter = sexRepositoryAdapter;
    }

    public void delete(Long id) {
        sexRepositoryAdapter.findById(id)
        .orElseThrow(() -> new NotFoundException("Sexo não encontrado para exclusão!"));
        sexRepositoryAdapter.deleteById(id);
    }
}
