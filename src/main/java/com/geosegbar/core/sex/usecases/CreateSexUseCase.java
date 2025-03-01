package com.geosegbar.core.sex.usecases;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.exceptions.DuplicateResourceException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public SexEntity create(SexEntity sexEntity) {

        if (sexRepositoryAdapter.existsByName(sexEntity.getName())) {
            throw new DuplicateResourceException("Já existe um sexo com este nome!");
        }

        return sexRepositoryAdapter.save(sexEntity);
    }
}
