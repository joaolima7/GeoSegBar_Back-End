package com.geosegbar.infra.dam_permissions.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.dam_permissions.dtos.DamPermissionDTO;
import com.geosegbar.infra.dam_permissions.dtos.UserDamPermissionsRequestDTO;
import com.geosegbar.infra.dam_permissions.persistence.DamPermissionRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DamPermissionService {

    private final DamPermissionRepository damPermissionRepository;
    private final UserRepository userRepository;
    private final DamRepository damRepository;
    private final ClientRepository clientRepository;
    
    public List<DamPermissionEntity> findAllDamPermissionsForUserClients(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));
        
        Set<ClientEntity> userClients = user.getClients();
        if (userClients.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<DamPermissionEntity> allPermissions = new ArrayList<>();
        
        for (ClientEntity client : userClients) {
            List<DamEntity> clientDams = damRepository.findByClient(client);
            
            for (DamEntity dam : clientDams) {
                var permission = damPermissionRepository.findByUserAndDamAndClient(user, dam, client);
                
                if (permission.isPresent()) {
                    allPermissions.add(permission.get());
                } else {
                    DamPermissionEntity newPermission = new DamPermissionEntity();
                    newPermission.setUser(user);
                    newPermission.setDam(dam);
                    newPermission.setClient(client);
                    newPermission.setHasAccess(false); 
                    newPermission.setCreatedAt(LocalDateTime.now());
                    
                    DamPermissionEntity savedPermission = damPermissionRepository.save(newPermission);
                    allPermissions.add(savedPermission);
                }
            }
        }
        
        return allPermissions;
    }
    
    @Transactional
    public List<DamPermissionEntity> setupPermissionsForUser(Long userId, UserDamPermissionsRequestDTO requestDTO) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));
        
        List<DamPermissionEntity> createdPermissions = new ArrayList<>();
        Set<String> processedKeys = new HashSet<>();
        
        for (DamPermissionDTO permissionDTO : requestDTO.getPermissions()) {
            permissionDTO.setUserId(userId);
            
            DamEntity dam = damRepository.findById(permissionDTO.getDamId())
                    .orElseThrow(() -> new NotFoundException(
                            "Barragem não encontrada com ID: " + permissionDTO.getDamId()));
            
            ClientEntity client = clientRepository.findById(permissionDTO.getClientId())
                    .orElseThrow(() -> new NotFoundException(
                            "Cliente não encontrado com ID: " + permissionDTO.getClientId()));
            
            if (!user.getClients().contains(client)) {
                throw new InvalidInputException("O cliente " + client.getName() + 
                        " não está associado ao usuário " + user.getName() + "!");
            }
            
            if (dam.getClient() == null || !dam.getClient().getId().equals(client.getId())) {
                throw new InvalidInputException("A barragem " + dam.getName() + 
                        " não está associada ao cliente " + client.getName() + "!");
            }
            
            String key = userId + "-" + permissionDTO.getDamId() + "-" + permissionDTO.getClientId();
            
            if (processedKeys.contains(key)) {
                continue;
            }
            
            processedKeys.add(key);
            
            var existingPermission = damPermissionRepository.findByUserAndDamAndClient(user, dam, client);
            
            if (existingPermission.isPresent()) {
                DamPermissionEntity permission = existingPermission.get();
                permission.setHasAccess(permissionDTO.getHasAccess());
                permission.setUpdatedAt(LocalDateTime.now());
                createdPermissions.add(damPermissionRepository.save(permission));
            } else {
                DamPermissionEntity permission = new DamPermissionEntity();
                permission.setUser(user);
                permission.setDam(dam);
                permission.setClient(client);
                permission.setHasAccess(permissionDTO.getHasAccess());
                permission.setCreatedAt(LocalDateTime.now());
                createdPermissions.add(damPermissionRepository.save(permission));
            }
        }
        
        return createdPermissions;
    }
    
    @Transactional
    public List<DamPermissionEntity> updatePermissionsForUser(Long userId, UserDamPermissionsRequestDTO requestDTO) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));
        
        List<DamPermissionEntity> updatedPermissions = new ArrayList<>();
        Set<String> processedKeys = new HashSet<>();
        
        List<DamPermissionEntity> currentPermissions = findAllDamPermissionsForUserClients(userId);
        Set<Long> updatedPermissionIds = new HashSet<>();
        
        for (DamPermissionDTO permissionDTO : requestDTO.getPermissions()) {
            permissionDTO.setUserId(userId);
            
            DamEntity dam = damRepository.findById(permissionDTO.getDamId())
                    .orElseThrow(() -> new NotFoundException(
                            "Barragem não encontrada com ID: " + permissionDTO.getDamId()));
            
            ClientEntity client = clientRepository.findById(permissionDTO.getClientId())
                    .orElseThrow(() -> new NotFoundException(
                            "Cliente não encontrado com ID: " + permissionDTO.getClientId()));
            
            if (!user.getClients().contains(client)) {
                throw new InvalidInputException("O cliente " + client.getName() + 
                        " não está associado ao usuário " + user.getName() + "!");
            }
            
            if (dam.getClient() == null || !dam.getClient().getId().equals(client.getId())) {
                throw new InvalidInputException("A barragem " + dam.getName() + 
                        " não está associada ao cliente " + client.getName() + "!");
            }
            
            String key = userId + "-" + permissionDTO.getDamId() + "-" + permissionDTO.getClientId();
            
            if (processedKeys.contains(key)) {
                continue;
            }
            
            processedKeys.add(key);
            
            var existingPermission = damPermissionRepository.findByUserAndDamAndClient(user, dam, client);
            
            if (existingPermission.isPresent()) {
                DamPermissionEntity permission = existingPermission.get();
                permission.setHasAccess(permissionDTO.getHasAccess());
                permission.setUpdatedAt(LocalDateTime.now());
                DamPermissionEntity updatedPermission = damPermissionRepository.save(permission);
                updatedPermissions.add(updatedPermission);
                updatedPermissionIds.add(updatedPermission.getId());
            } else {
                DamPermissionEntity permission = new DamPermissionEntity();
                permission.setUser(user);
                permission.setDam(dam);
                permission.setClient(client);
                permission.setHasAccess(permissionDTO.getHasAccess());
                permission.setCreatedAt(LocalDateTime.now());
                DamPermissionEntity savedPermission = damPermissionRepository.save(permission);
                updatedPermissions.add(savedPermission);
                updatedPermissionIds.add(savedPermission.getId());
            }
        }
        
        for (DamPermissionEntity currentPermission : currentPermissions) {
            if (!updatedPermissionIds.contains(currentPermission.getId())) {
                currentPermission.setHasAccess(false);
                currentPermission.setUpdatedAt(LocalDateTime.now());
                updatedPermissions.add(damPermissionRepository.save(currentPermission));
            }
        }
        
        return updatedPermissions;
    }
    
    @Transactional
    public void delete(Long id) {
        if (!damPermissionRepository.existsById(id)) {
            throw new NotFoundException("Permissão de barragem não encontrada com ID: " + id);
        }
        damPermissionRepository.deleteById(id);
    }
    
    public boolean checkUserHasAccessToDam(Long userId, Long damId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));
                
        DamEntity dam = damRepository.findById(damId)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + damId));
        
        ClientEntity client = dam.getClient();
        if (client == null) {
            throw new NotFoundException("Barragem não está associada a nenhum cliente");
        }
        
        if (!user.getClients().contains(client)) {
            return false;
        }
        
        var permission = damPermissionRepository.findByUserAndDamAndClient(user, dam, client);
        if (permission.isPresent()) {
            return permission.get().getHasAccess();
        }
        
        return user.getRole() != null && "ADMIN".equals(user.getRole().getName().name());
    }
}