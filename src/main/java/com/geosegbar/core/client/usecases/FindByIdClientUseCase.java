package com.geosegbar.core.client.usecases;

import com.geosegbar.adapters.client.ClientRepositoryAdapter;
import com.geosegbar.core.client.entities.ClientEntity;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindByIdClientUseCase {
    
    private final ClientRepositoryAdapter clientRepositoryAdapter;

    public ClientEntity findById(Long id){
        return clientRepositoryAdapter.findById(id).
        orElseThrow(() -> new NotFoundException("Cliente não encontrado!"));
    }
}
