package com.geosegbar.infra.client.service;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.dtos.ClientDTO;
import com.geosegbar.infra.client.dtos.ClientStatusUpdateDTO;
import com.geosegbar.infra.client.dtos.LogoUpdateDTO;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.client.utils.ClientStatusChangeHandler;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;
import com.geosegbar.infra.user.dto.UserClientAssociationDTO;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;
import com.geosegbar.infra.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final StatusRepository statusRepository;
    private final ClientStatusChangeHandler statusChangeHandler;

    @Transactional
    public void deleteById(Long id) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado para exclusão!"));

        if (clientRepository.countDamsByClientId(id) > 0
                || clientRepository.countDamPermissionsByClientId(id) > 0
                || clientRepository.countUsersByClientId(id) > 0) {
            throw new BusinessRuleException(
                    "Não é possível excluir cliente devido as dependências existentes, recomenda-se inativar o cliente se necessário.");
        }

        if (client.getLogoPath() != null) {
            fileStorageService.deleteFile(client.getLogoPath());
        }

        clientRepository.deleteById(id);
    }

    @Transactional
    public ClientEntity updateStatus(Long clientId, ClientStatusUpdateDTO statusUpdateDTO) {
        ClientEntity client = findById(clientId);

        StatusEntity newStatus = statusRepository.findById(statusUpdateDTO.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status não encontrado com ID: " + statusUpdateDTO.getStatusId()));

        statusChangeHandler.handleStatusChange(client, newStatus);

        client.setStatus(newStatus);

        return clientRepository.save(client);
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

        if (clientDTO.getStatus() != null && clientDTO.getStatus().getId() != null) {
            StatusEntity status = statusRepository.findById(clientDTO.getStatus().getId())
                    .orElseThrow(() -> new NotFoundException("Status não encontrado!"));
            clientEntity.setStatus(status);
        }

        processLogoUpdate(clientEntity, clientDTO.getLogoBase64(), null);

        ClientEntity savedClient = clientRepository.save(clientEntity);

        if (clientDTO.getUserIds() != null && !clientDTO.getUserIds().isEmpty()) {
            associateUsersToClient(savedClient, clientDTO.getUserIds());
        }

        return findById(savedClient.getId());
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

        updateEntityFromDTO(existingClient, clientDTO);

        if (clientDTO.getStatus() != null && clientDTO.getStatus().getId() != null) {
            StatusEntity status = statusRepository.findById(clientDTO.getStatus().getId())
                    .orElseThrow(() -> new NotFoundException("Status não encontrado!"));

            if (existingClient.getStatus() != null && !existingClient.getStatus().getId().equals(status.getId())) {
                statusChangeHandler.handleStatusChange(existingClient, status);
            }
            existingClient.setStatus(status);
        }

        processLogoUpdate(existingClient, clientDTO.getLogoBase64(), existingClient);

        ClientEntity savedClient = clientRepository.save(existingClient);

        processUserAssociations(savedClient, clientDTO.getUserIds());

        return findById(savedClient.getId());
    }

    private void associateUsersToClient(ClientEntity client, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        List<UserEntity> users = userRepository.findByIdInWithClients(userIds);

        if (users.size() != userIds.size()) {
            Set<Long> foundIds = users.stream().map(UserEntity::getId).collect(Collectors.toSet());
            Set<Long> missingIds = new HashSet<>(userIds);
            missingIds.removeAll(foundIds);
            throw new NotFoundException("Usuários não encontrados com IDs: " + missingIds);
        }

        validateClientStatusForAssociation(client, users);

        for (UserEntity user : users) {
            UserClientAssociationDTO associationDTO = new UserClientAssociationDTO();
            Set<Long> clientIdsForUser = new HashSet<>();
            clientIdsForUser.add(client.getId());
            associationDTO.setClientIds(clientIdsForUser);

            userService.updateUserClients(user.getId(), associationDTO);
        }
    }

    private void processUserAssociations(ClientEntity client, Set<Long> userIds) {
        if (userIds == null) {
            return;
        }

        List<UserEntity> currentUsers = userRepository.findByClientId(client.getId());

        if (userIds.isEmpty()) {
            for (UserEntity user : currentUsers) {
                UserClientAssociationDTO associationDTO = new UserClientAssociationDTO();
                associationDTO.setClientIds(new HashSet<>());
                userService.updateUserClients(user.getId(), associationDTO);
            }
            return;
        }

        Set<Long> currentUserIds = currentUsers.stream().map(UserEntity::getId).collect(Collectors.toSet());

        Set<Long> usersToAdd = new HashSet<>(userIds);
        usersToAdd.removeAll(currentUserIds);

        Set<Long> usersToRemove = new HashSet<>(currentUserIds);
        usersToRemove.removeAll(userIds);

        if (!usersToRemove.isEmpty()) {
            List<UserEntity> usersToRemoveList = currentUsers.stream()
                    .filter(u -> usersToRemove.contains(u.getId()))
                    .collect(Collectors.toList());

            for (UserEntity user : usersToRemoveList) {
                UserClientAssociationDTO associationDTO = new UserClientAssociationDTO();
                associationDTO.setClientIds(new HashSet<>());
                userService.updateUserClients(user.getId(), associationDTO);
            }
        }

        if (!usersToAdd.isEmpty()) {
            List<UserEntity> usersToAddList = userRepository.findByIdInWithClients(usersToAdd);

            if (usersToAddList.size() != usersToAdd.size()) {
                Set<Long> foundIds = usersToAddList.stream().map(UserEntity::getId).collect(Collectors.toSet());
                Set<Long> missingIds = new HashSet<>(usersToAdd);
                missingIds.removeAll(foundIds);
                throw new NotFoundException("Usuários não encontrados com IDs: " + missingIds);
            }

            validateClientStatusForAssociation(client, usersToAddList);

            for (UserEntity user : usersToAddList) {
                UserClientAssociationDTO associationDTO = new UserClientAssociationDTO();
                Set<Long> clientIdsForUser = new HashSet<>();
                clientIdsForUser.add(client.getId());
                associationDTO.setClientIds(clientIdsForUser);
                userService.updateUserClients(user.getId(), associationDTO);
            }
        }
    }

    private void validateClientStatusForAssociation(ClientEntity client, List<UserEntity> users) {

        if (client.getStatus() != null && "DISABLED".equals(client.getStatus().getStatus().name())) {
            for (UserEntity user : users) {
                if (user.getStatus() != null && "ACTIVE".equals(user.getStatus().getStatus().name())) {
                    throw new BusinessRuleException(
                            "Não é possível associar o usuário '" + user.getName()
                            + "' ao cliente pois este cliente está desativado. Desative o usuário primeiro ou ative o cliente."
                    );
                }
            }
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
                }else if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50) {
                    contentType = "image/png"; 
                }else if (imageBytes[0] == (byte) 0x47 && imageBytes[1] == (byte) 0x49) {
                    contentType = "image/gif";
                }
            }
            String fileExtension = contentType.equals("image/jpeg") ? ".jpg" : ".png";
            String fileName = "client_logo_" + System.currentTimeMillis() + fileExtension;
            return fileStorageService.storeFileFromBytes(imageBytes, fileName, contentType, "client-logos");
        } catch (IllegalArgumentException e) {
            throw new FileStorageException("Formato de imagem inválido: " + e.getMessage(), e);
        }
    }

    public ClientEntity findById(Long id) {
        return clientRepository.findById(id).orElseThrow(() -> new NotFoundException("Cliente não encontrado!"));
    }

    public List<ClientEntity> findAll() {
        return clientRepository.findAllByOrderByIdAsc();
    }

    public List<ClientEntity> findByStatus(Long statusId) {
        if (statusId == null) {
            throw new NotFoundException("Status não informado!");
        }
        return clientRepository.findByStatus(statusId);
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
        updateEntityFromDTO(entity, clientDTO);
        return entity;
    }

    private void updateEntityFromDTO(ClientEntity entity, ClientDTO clientDTO) {
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

    }

    private void processLogoUpdate(ClientEntity clientEntity, String logoBase64, ClientEntity existingClient) {
        if (logoBase64 == null) {
            if (existingClient != null) {
                clientEntity.setLogoPath(existingClient.getLogoPath()); 
            }else {
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
