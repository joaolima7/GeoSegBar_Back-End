package com.geosegbar.infra.client.service;

import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.dtos.LogoUpdateDTO;
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
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado para exclusão!"));

        if (client.getLogoPath() != null) {
            fileStorageService.deleteFile(client.getLogoPath());
        }

        clientRepository.deleteById(id);
    }

    @Transactional
    public ClientEntity save(ClientEntity clientEntity) {
        if (clientRepository.existsByName(clientEntity.getName())) {
            throw new DuplicateResourceException("Já existe um cliente com este nome!");
        }

        if (clientRepository.existsByEmail(clientEntity.getEmail())) {
            throw new DuplicateResourceException("Já existe um cliente com este email!");
        }

        String logoBase64 = clientEntity.getLogoPath();

        if (logoBase64 != null) {
            String logoPath = processAndSaveLogo(logoBase64);
            clientEntity.setLogoPath(logoPath);
        } else {
            clientEntity.setLogoPath(null);
        }

        return clientRepository.save(clientEntity);
    }

    @Transactional
    public ClientEntity update(ClientEntity clientEntity) {
        ClientEntity existingClient = clientRepository.findById(clientEntity.getId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado para atualização!"));

        if (clientRepository.existsByNameAndIdNot(clientEntity.getName(), clientEntity.getId())) {
            throw new DuplicateResourceException("Já existe um cliente com este nome.");
        }

        if (clientRepository.existsByEmailAndIdNot(clientEntity.getEmail(), clientEntity.getId())) {
            throw new DuplicateResourceException("Já existe um cliente com este email.");
        }

        if (existingClient.getLogoPath() != null) {
            clientEntity.setLogoPath(existingClient.getLogoPath());
        } else {
            clientEntity.setLogoPath(null);
        }

        return clientRepository.save(clientEntity);
    }

    @Transactional
    public ClientEntity updateLogo(Long clientId, LogoUpdateDTO logoUpdateDTO) {
        ClientEntity client = findById(clientId);

        if (client.getLogoPath() != null) {
            fileStorageService.deleteFile(client.getLogoPath());
            client.setLogoPath(null);
        }

        String logoBase64 = logoUpdateDTO.getLogoBase64();

        if (logoBase64 != null) {
            String logoPath = processAndSaveLogo(logoBase64);
            client.setLogoPath(logoPath);
        }

        return clientRepository.save(client);
    }

    private String processAndSaveLogo(String base64Image) {
        try {
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            return fileStorageService.storeFileFromBytes(
                    imageBytes,
                    "client_logo.png",
                    "image/png",
                    "client-logos"
            );
        } catch (IllegalArgumentException e) {
            throw new FileStorageException("Formato de imagem inválido", e);
        }
    }

    public ClientEntity findById(Long id) {
        return clientRepository.findById(id).
                orElseThrow(() -> new NotFoundException("Cliente não encontrado!"));
    }

    public List<ClientEntity> findAll() {
        return clientRepository.findAllByOrderByIdAsc();
    }

    public List<ClientEntity> findByStatus(Long statusId) {
        if (statusId == null) {
            throw new NotFoundException("Status não informado para filtro de clientes!");
        }

        List<ClientEntity> clients = clientRepository.findByStatus(statusId);

        return clients;
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
