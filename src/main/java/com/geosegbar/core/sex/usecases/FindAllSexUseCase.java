package com.geosegbar.core.sex.usecases;

import java.util.List;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;

public class FindAllSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public FindAllSexUseCase(SexRepositoryAdapter sexRepositoryAdapter) {
        this.sexRepositoryAdapter = sexRepositoryAdapter;
    }

    public List<SexEntity> findAll() {
        return sexRepositoryAdapter.findAll();
    }
}
