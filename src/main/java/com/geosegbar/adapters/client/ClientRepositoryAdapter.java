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

    boolean existsByName(String name);
    boolean existsByAcronym(String acronym);
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByAcronymAndIdNot(String acronym, Long id);
}
