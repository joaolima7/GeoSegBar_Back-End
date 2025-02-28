package com.geosegbar.core.client.usecases;

import com.geosegbar.adapters.client.ClientRepositoryAdapter;
import com.geosegbar.core.client.entities.ClientEntity;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateCientUseCase {
    
    private final ClientRepositoryAdapter clientRepositoryAdapter;

    public ClientEntity update(ClientEntity clientEntity){
        clientRepositoryAdapter.findById(clientEntity.getId()).
        orElseThrow(() -> new NotFoundException("Endereço não encontrado para atualização!"));
        return clientRepositoryAdapter.save(clientEntity);
    }
}
