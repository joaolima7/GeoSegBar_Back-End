package com.geosegbar.infra.client.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.file_storage.FileStorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void deleteById(Long id) {
        clientRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado para exclusão!"));
        clientRepository.deleteById(id);
    }

    @Transactional
    public ClientEntity save(ClientEntity clientEntity) {
        if (clientRepository.existsByName(clientEntity.getName())) {
            throw new DuplicateResourceException("Já existe um cliente com este nome!");
        }

        if(clientRepository.existsByEmail(clientEntity.getEmail())){
            throw new DuplicateResourceException("Já existe um cliente com este email!");
        }
        return clientRepository.save(clientEntity);
    }

    @Transactional
    public ClientEntity update(ClientEntity clientEntity) {
        clientRepository.findById(clientEntity.getId()).
        orElseThrow(() -> new NotFoundException("Endereço não encontrado para atualização!"));

        if (clientRepository.existsByNameAndIdNot(clientEntity.getName(), clientEntity.getId())) {
            throw new DuplicateResourceException("Já existe um cliente com este nome.");
        }

        if(clientRepository.existsByEmailAndIdNot(clientEntity.getEmail(), clientEntity.getId())){
            throw new DuplicateResourceException("Já existe um cliente com este email.");
        }

        return clientRepository.save(clientEntity);
    }

        @Transactional
    public ClientEntity saveLogo(Long clientId, MultipartFile logo) {
        ClientEntity client = findById(clientId);
        
        if (client.getLogoPath() != null) {
            fileStorageService.deleteFile(client.getLogoPath());
        }
        
        String logoUrl = fileStorageService.storeFile(logo, "client-logos");
        client.setLogoPath(logoUrl);
        
        return clientRepository.save(client);
    }

    public ClientEntity findById(Long id) {
        return clientRepository.findById(id).
        orElseThrow(() -> new NotFoundException("Cliente não encontrado!"));
    }

    public List<ClientEntity> findAll() {
        return clientRepository.findAllByOrderByIdAsc();
    }

    public boolean existsByName(String name) {
        return clientRepository.existsByName(name);
    }

    public boolean existsByNameAndIdNot(String name, Long id) {
        return clientRepository.existsByNameAndIdNot(name, id);
    }

    public boolean existsByEmail(String email) {
        return clientRepository.existsByEmail(email);
    }

    public boolean existsByEmailAndIdNot(String email, Long id) {
        return clientRepository.existsByEmailAndIdNot(email, id);
    }
    
}
