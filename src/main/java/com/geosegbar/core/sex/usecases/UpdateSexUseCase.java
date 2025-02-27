package com.geosegbar.core.sex.usecases;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.exceptions.NotFoundException;

public class UpdateSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public UpdateSexUseCase(SexRepositoryAdapter sexRepositoryAdapter) {
        this.sexRepositoryAdapter = sexRepositoryAdapter;
    }

    public SexEntity update(SexEntity sexEntity){
        sexRepositoryAdapter.findById(sexEntity.getId()).
        orElseThrow(() -> new NotFoundException("Sexo não encontrado para atualização!"));
        return sexRepositoryAdapter.update(sexEntity);
    }
}
