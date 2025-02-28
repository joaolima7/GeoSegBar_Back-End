package com.geosegbar.core.client.usecases;

import com.geosegbar.adapters.client.ClientRepositoryAdapter;
import com.geosegbar.core.client.entities.ClientEntity;
import com.geosegbar.exceptions.DuplicateResourceException;

import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
public class CreateClientUseCase {
    private final ClientRepositoryAdapter clientRepositoryAdapter;

    public ClientEntity create(ClientEntity clientEntity) {

        if (clientRepositoryAdapter.existsByName(clientEntity.getName())) {
            throw new DuplicateResourceException("Já existe um cliente com este nome.");
        }
        
        if (clientRepositoryAdapter.existsByAcronym(clientEntity.getAcronym())) {
            throw new DuplicateResourceException("Já existe um cliente com esta sigla.");
        }

       return clientRepositoryAdapter.save(clientEntity);
    }
}
