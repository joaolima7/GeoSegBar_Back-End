package com.geosegbar.core.client.usecases;

import com.geosegbar.adapters.client.ClientRepositoryAdapter;
import com.geosegbar.core.client.entities.ClientEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateClientUseCase {
    
    private final ClientRepositoryAdapter clientRepositoryAdapter;

    public ClientEntity update(ClientEntity clientEntity){
        clientRepositoryAdapter.findById(clientEntity.getId()).
        orElseThrow(() -> new NotFoundException("Endereço não encontrado para atualização!"));

        if (clientRepositoryAdapter.existsByNameAndIdNot(clientEntity.getName(), clientEntity.getId())) {
            throw new DuplicateResourceException("Já existe um cliente com este nome.");
        }
        
        if (clientRepositoryAdapter.existsByAcronymAndIdNot(clientEntity.getAcronym(), clientEntity.getId())) {
            throw new DuplicateResourceException("Já existe um cliente com esta sigla.");
        }
        
        return clientRepositoryAdapter.save(clientEntity);
    }
}
