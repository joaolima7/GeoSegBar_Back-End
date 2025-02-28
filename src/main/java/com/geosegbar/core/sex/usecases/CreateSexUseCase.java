package com.geosegbar.core.sex.usecases;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public SexEntity create(SexEntity sexEntity) {
        return sexRepositoryAdapter.save(sexEntity);
    }
}
