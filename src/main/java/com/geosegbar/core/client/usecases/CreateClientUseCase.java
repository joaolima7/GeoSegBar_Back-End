package com.geosegbar.core.client.usecases;

import com.geosegbar.adapters.client.ClientRepositoryAdapter;
import com.geosegbar.core.client.entities.ClientEntity;

import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
public class CreateClientUseCase {
    private final ClientRepositoryAdapter clientRepositoryAdapter;

    public ClientEntity create(ClientEntity clientEntity) {
       return clientRepositoryAdapter.save(clientEntity);
    }
}
