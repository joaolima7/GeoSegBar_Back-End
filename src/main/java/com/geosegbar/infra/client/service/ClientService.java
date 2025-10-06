package com.geosegbar.infra.client.service;

import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.dtos.ClientDTO;
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
    public ClientEntity save(ClientDTO clientDTO) {
        if (clientRepository.existsByName(clientDTO.getName())) {
            throw new DuplicateResourceException("Já existe um cliente com este nome!");
        }

        if (clientRepository.existsByEmail(clientDTO.getEmail())) {
            throw new DuplicateResourceException("Já existe um cliente com este email!");
        }

        ClientEntity clientEntity = convertDTOToEntity(clientDTO);

        processLogoUpdate(clientEntity, clientDTO.getLogoBase64(), null);

        return clientRepository.save(clientEntity);
    }

    @Transactional
    public ClientEntity update(ClientDTO clientDTO) {
        ClientEntity existingClient = clientRepository.findById(clientDTO.getId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado para atualização!"));

        if (clientRepository.existsByNameAndIdNot(clientDTO.getName(), clientDTO.getId())) {
            throw new DuplicateResourceException("Já existe um cliente com este nome.");
        }

        if (clientRepository.existsByEmailAndIdNot(clientDTO.getEmail(), clientDTO.getId())) {
            throw new DuplicateResourceException("Já existe um cliente com este email.");
        }

        ClientEntity clientEntity = convertDTOToEntity(clientDTO);

        processLogoUpdate(clientEntity, clientDTO.getLogoBase64(), existingClient);

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

            base64Image = base64Image.trim().replaceAll("\\s", "");

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            String contentType = "image/png";
            if (imageBytes.length > 2) {
                if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8) {
                    contentType = "image/jpeg";
                } else if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50) {
                    contentType = "image/png";
                } else if (imageBytes[0] == (byte) 0x47 && imageBytes[1] == (byte) 0x49) {
                    contentType = "image/gif";
                }
            }

            String fileExtension = contentType.equals("image/jpeg") ? ".jpg" : ".png";
            String fileName = "client_logo_" + System.currentTimeMillis() + fileExtension;

            return fileStorageService.storeFileFromBytes(
                    imageBytes,
                    fileName,
                    contentType,
                    "client-logos"
            );
        } catch (IllegalArgumentException e) {
            throw new FileStorageException("Formato de imagem inválido: " + e.getMessage(), e);
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

    private ClientEntity convertDTOToEntity(ClientDTO clientDTO) {
        ClientEntity entity = new ClientEntity();
        entity.setId(clientDTO.getId());
        entity.setName(clientDTO.getName());
        entity.setEmail(clientDTO.getEmail());
        entity.setStreet(clientDTO.getStreet());
        entity.setNeighborhood(clientDTO.getNeighborhood());
        entity.setNumberAddress(clientDTO.getNumberAddress());
        entity.setCity(clientDTO.getCity());
        entity.setState(clientDTO.getState());
        entity.setZipCode(clientDTO.getZipCode());
        entity.setComplement(clientDTO.getComplement());
        entity.setPhone(clientDTO.getPhone());
        entity.setWhatsappPhone(clientDTO.getWhatsappPhone());
        entity.setEmailContact(clientDTO.getEmailContact());
        entity.setStatus(clientDTO.getStatus());

        return entity;
    }

    private void processLogoUpdate(ClientEntity clientEntity, String logoBase64, ClientEntity existingClient) {

        if (logoBase64 == null) {
            if (existingClient != null) {
                clientEntity.setLogoPath(existingClient.getLogoPath());
            } else {
                clientEntity.setLogoPath(null);
            }
            return;
        }

        if (logoBase64.trim().isEmpty() || "null".equalsIgnoreCase(logoBase64.trim())) {
            if (existingClient != null && existingClient.getLogoPath() != null) {
                fileStorageService.deleteFile(existingClient.getLogoPath());
            }
            clientEntity.setLogoPath(null);
            return;
        }

        try {

            if (existingClient != null && existingClient.getLogoPath() != null) {
                fileStorageService.deleteFile(existingClient.getLogoPath());
            }

            String logoPath = processAndSaveLogo(logoBase64);
            clientEntity.setLogoPath(logoPath);

        } catch (Exception e) {
            throw new FileStorageException("Erro ao processar logo: " + e.getMessage(), e);
        }
    }

}
