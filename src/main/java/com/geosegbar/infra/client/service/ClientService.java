package com.geosegbar.infra.client.service;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.dtos.ClientDTO;
import com.geosegbar.infra.client.dtos.LogoUpdateDTO;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

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

        // Processar logo
        processLogoUpdate(clientEntity, clientDTO.getLogoBase64(), null);

        // Salvar cliente primeiro para obter o ID
        ClientEntity savedClient = clientRepository.save(clientEntity);

        // Associar usuários ao cliente
        if (clientDTO.getUserIds() != null && !clientDTO.getUserIds().isEmpty()) {
            associateUsersToClient(savedClient, clientDTO.getUserIds());
        }

        return savedClient;
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

        // Processar logo
        processLogoUpdate(clientEntity, clientDTO.getLogoBase64(), existingClient);

        // Salvar cliente atualizado
        ClientEntity savedClient = clientRepository.save(clientEntity);

        // Processar associação de usuários
        processUserAssociations(savedClient, clientDTO.getUserIds());

        return savedClient;
    }

    private void associateUsersToClient(ClientEntity client, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        // Buscar usuários por IDs (batch query)
        List<UserEntity> users = userRepository.findByIdInWithClients(userIds);

        if (users.size() != userIds.size()) {
            Set<Long> foundIds = users.stream()
                    .map(UserEntity::getId)
                    .collect(Collectors.toSet());
            Set<Long> missingIds = new HashSet<>(userIds);
            missingIds.removeAll(foundIds);
            throw new NotFoundException("Usuários não encontrados com IDs: " + missingIds);
        }

        // Para cada usuário, remover este cliente se já existir e adicionar novamente
        for (UserEntity user : users) {
            // Remover o cliente atual de outros clientes do usuário se necessário
            user.getClients().clear();
            user.getClients().add(client);
        }

        // Salvar todos de uma vez (batch update)
        userRepository.saveAll(users);
    }

    private void processUserAssociations(ClientEntity client, Set<Long> userIds) {
        // Se userIds é null, não modificar associações
        if (userIds == null) {
            return;
        }

        // Buscar usuários atualmente associados a este cliente
        List<UserEntity> currentUsers = userRepository.findByClientId(client.getId());

        // Se userIds está vazio, remover todas as associações
        if (userIds.isEmpty()) {
            for (UserEntity user : currentUsers) {
                user.getClients().remove(client);
            }
            userRepository.saveAll(currentUsers);
            return;
        }

        // Obter IDs dos usuários atualmente associados
        Set<Long> currentUserIds = currentUsers.stream()
                .map(UserEntity::getId)
                .collect(Collectors.toSet());

        // Identificar usuários a serem adicionados e removidos
        Set<Long> usersToAdd = new HashSet<>(userIds);
        usersToAdd.removeAll(currentUserIds);

        Set<Long> usersToRemove = new HashSet<>(currentUserIds);
        usersToRemove.removeAll(userIds);

        // Remover usuários
        if (!usersToRemove.isEmpty()) {
            List<UserEntity> usersToRemoveList = currentUsers.stream()
                    .filter(u -> usersToRemove.contains(u.getId()))
                    .collect(Collectors.toList());

            for (UserEntity user : usersToRemoveList) {
                user.getClients().remove(client);
            }
            userRepository.saveAll(usersToRemoveList);
        }

        // Adicionar novos usuários
        if (!usersToAdd.isEmpty()) {
            List<UserEntity> usersToAddList = userRepository.findByIdInWithClients(usersToAdd);

            if (usersToAddList.size() != usersToAdd.size()) {
                Set<Long> foundIds = usersToAddList.stream()
                        .map(UserEntity::getId)
                        .collect(Collectors.toSet());
                Set<Long> missingIds = new HashSet<>(usersToAdd);
                missingIds.removeAll(foundIds);
                throw new NotFoundException("Usuários não encontrados com IDs: " + missingIds);
            }

            for (UserEntity user : usersToAddList) {
                // Limpar clientes anteriores e adicionar apenas este
                user.getClients().clear();
                user.getClients().add(client);
            }
            userRepository.saveAll(usersToAddList);
        }
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
