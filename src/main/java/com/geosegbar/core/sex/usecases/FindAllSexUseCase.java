package com.geosegbar.core.sex.usecases;

import java.util.List;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public List<SexEntity> findAll() {
        return sexRepositoryAdapter.findAll();
    }
}
