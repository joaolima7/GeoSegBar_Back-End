package com.geosegbar.core.sex.usecases;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;

public class CreateSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public CreateSexUseCase(SexRepositoryAdapter sexRepositoryAdapter) {
        this.sexRepositoryAdapter = sexRepositoryAdapter;
    }

    public SexEntity create(SexEntity sexEntity) {
        return sexRepositoryAdapter.save(sexEntity);
    }
}
