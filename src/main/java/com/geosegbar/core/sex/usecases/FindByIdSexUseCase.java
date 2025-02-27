package com.geosegbar.core.sex.usecases;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.exceptions.NotFoundException;

public class FindByIdSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public FindByIdSexUseCase(SexRepositoryAdapter sexRepositoryAdapter) {
        this.sexRepositoryAdapter = sexRepositoryAdapter;
    }

    public SexEntity findById(Long id) {
        return sexRepositoryAdapter.findById(id).
        orElseThrow(() -> new NotFoundException("Sexo não encontrado!"));
    }
}
