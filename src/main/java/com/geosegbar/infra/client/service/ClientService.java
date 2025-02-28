package com.geosegbar.infra.client.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.core.client.entities.ClientEntity;
import com.geosegbar.core.client.usecases.CreateClientUseCase;
import com.geosegbar.core.client.usecases.DeleteClientUseCase;
import com.geosegbar.core.client.usecases.FindAllClientUseCase;
import com.geosegbar.core.client.usecases.FindByIdClientUseCase;
import com.geosegbar.core.client.usecases.UpdateClientUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final FindAllClientUseCase findAllClientUseCase;
    private final FindByIdClientUseCase findByIdClientUseCase;
    private final CreateClientUseCase createClientUseCase;
    private final UpdateClientUseCase updateClientUseCase;
    private final DeleteClientUseCase deleteClientUseCase;

    public ClientEntity create(ClientEntity client) {
        return createClientUseCase.create(client);
    }

    public ClientEntity update(ClientEntity client) {
        return updateClientUseCase.update(client);
    }

    public void delete(Long id) {
         deleteClientUseCase.delete(id);
    }

    public ClientEntity findById(Long id) {
         return findByIdClientUseCase.findById(id);
    }

    public List<ClientEntity> findAll() {
         return findAllClientUseCase.findAll();
    }
    
}
