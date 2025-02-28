package com.geosegbar.core.sex.usecases;

import com.geosegbar.adapters.sex.SexRepositoryAdapter;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteSexUseCase {
    private final SexRepositoryAdapter sexRepositoryAdapter;

    public void delete(Long id) {
        sexRepositoryAdapter.findById(id)
        .orElseThrow(() -> new NotFoundException("Sexo não encontrado para exclusão!"));
        sexRepositoryAdapter.deleteById(id);
    }
}
