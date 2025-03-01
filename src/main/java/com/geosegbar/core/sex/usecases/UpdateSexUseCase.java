package com.geosegbar.core.sex.usecases;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public SexEntity update(SexEntity sexEntity){
        sexRepositoryAdapter.findById(sexEntity.getId()).
        orElseThrow(() -> new NotFoundException("Sexo não encontrado para atualização!"));

        if(sexRepositoryAdapter.existsByNameAndIdNot(sexEntity.getName(), sexEntity.getId())){
            throw new DuplicateResourceException("Já existe um sexo com este nome!");
        }

        return sexRepositoryAdapter.update(sexEntity);
    }
}
