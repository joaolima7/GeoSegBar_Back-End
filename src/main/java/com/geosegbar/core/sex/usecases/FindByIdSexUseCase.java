package com.geosegbar.core.sex.usecases;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindByIdSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public SexEntity findById(Long id) {
        return sexRepositoryAdapter.findById(id).
        orElseThrow(() -> new NotFoundException("Sexo não encontrado!"));
    }
}
