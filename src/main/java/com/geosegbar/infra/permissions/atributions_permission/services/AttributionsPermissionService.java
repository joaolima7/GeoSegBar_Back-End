package com.geosegbar.infra.permissions.atributions_permission.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.AttributionsPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.permissions.atributions_permission.dtos.AttributionsPermissionDTO;
import com.geosegbar.infra.permissions.atributions_permission.persistence.AttributionsPermissionRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttributionsPermissionService {

    private final AttributionsPermissionRepository attrPermissionRepository;
    private final UserRepository userRepository;

    public List<AttributionsPermissionEntity> findAll() {
        return attrPermissionRepository.findAll();
    }

    public AttributionsPermissionEntity findById(Long id) {
        return attrPermissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permissão de atribuições não encontrada com ID: " + id));
    }

    public AttributionsPermissionEntity findByUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        return attrPermissionRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Permissão de atribuições não encontrada para o usuário!"));
    }

    @Transactional
    public AttributionsPermissionEntity createOrUpdate(AttributionsPermissionDTO permissionDTO) {
        UserEntity user = userRepository.findById(permissionDTO.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + permissionDTO.getUserId()));

        AttributionsPermissionEntity permission;

        var existingPermission = attrPermissionRepository.findByUser(user);

        if (existingPermission.isPresent()) {
            permission = existingPermission.get();
        } else {
            permission = new AttributionsPermissionEntity();
            permission.setUser(user);
        }

        permission.setEditUser(permissionDTO.getEditUser());
        permission.setEditDam(permissionDTO.getEditDam());
        permission.setEditGeralData(permissionDTO.getEditGeralData());

        return attrPermissionRepository.save(permission);
    }

    @Transactional
    public void delete(Long id) {
        if (!attrPermissionRepository.existsById(id)) {
            throw new NotFoundException("Permissão de atribuições não encontrada com ID: " + id);
        }
        attrPermissionRepository.deleteById(id);
    }

    @Transactional
    public void deleteByUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        if (!attrPermissionRepository.existsByUser(user)) {
            throw new NotFoundException("Permissão de atribuições não encontrada para o usuário");
        }

        AttributionsPermissionEntity permission = attrPermissionRepository.findByUser(user).get();
        permission.setUser(null);
        attrPermissionRepository.delete(permission);
    }

    @Transactional
    public void deleteByUserSafely(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

            if (attrPermissionRepository.existsByUser(user)) {
                AttributionsPermissionEntity permission = attrPermissionRepository.findByUser(user).get();
                attrPermissionRepository.delete(permission);
            } else {
            }
        } catch (Exception e) {
            log.error("Error while trying to delete attributions permission for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    @Transactional
    public AttributionsPermissionEntity createDefaultPermission(UserEntity user) {

        if (attrPermissionRepository.existsByUser(user)) {
            return attrPermissionRepository.findByUser(user).get();
        }

        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setUser(user);
        permission.setEditUser(false);
        permission.setEditDam(false);
        permission.setEditGeralData(false);

        AttributionsPermissionEntity savedPermission = attrPermissionRepository.save(permission);
        return savedPermission;
    }
}
