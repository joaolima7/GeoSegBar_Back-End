package com.geosegbar.infra.client.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.geosegbar.adapters.client.ClientRepositoryAdapter;
import com.geosegbar.core.client.entities.ClientEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClientJpaRepositoryImp implements ClientRepositoryAdapter{
    
    private final ClientRepository clientRepository;

    @Override
    public void deleteById(Long id) {
        clientRepository.deleteById(id);
    }

    @Override
    public ClientEntity save(ClientEntity clientEntity) {
        return clientRepository.save(clientEntity);
    }

    @Override
    public ClientEntity update(ClientEntity clientEntity) {
        return clientRepository.save(clientEntity);
    }

    @Override
    public Optional<ClientEntity> findById(Long id) {
        return clientRepository.findById(id);
    }

    @Override
    public List<ClientEntity> findAll() {
        return clientRepository.findAllByOrderByIdAsc();
    }


}
