package com.geosegbar.core.client.usecases;

import java.util.List;

import com.geosegbar.adapters.client.ClientRepositoryAdapter;
import com.geosegbar.core.client.entities.ClientEntity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllClientUseCase {
    
    private final ClientRepositoryAdapter clientRepositoryAdapter;

    public List<ClientEntity> findAll() {
        return clientRepositoryAdapter.findAll();
    }
}
