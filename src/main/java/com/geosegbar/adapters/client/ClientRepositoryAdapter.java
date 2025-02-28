package com.geosegbar.adapters.client;

import java.util.List;
import java.util.Optional;

import com.geosegbar.core.client.entities.ClientEntity;

public interface ClientRepositoryAdapter {
    void deleteById(Long id);
    ClientEntity save(ClientEntity clientEntity);
    ClientEntity update(ClientEntity clientEntity);
    Optional<ClientEntity> findById(Long id);
    List<ClientEntity> findAll();
}
