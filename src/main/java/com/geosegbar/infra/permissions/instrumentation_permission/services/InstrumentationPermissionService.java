package com.geosegbar.infra.permissions.instrumentation_permission.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.permissions.instrumentation_permission.dtos.InstrumentationPermissionDTO;
import com.geosegbar.infra.permissions.instrumentation_permission.persistence.InstrumentationPermissionRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentationPermissionService {

    private final InstrumentationPermissionRepository instrPermissionRepository;
    private final UserRepository userRepository;

    public List<InstrumentationPermissionEntity> findAll() {
        return instrPermissionRepository.findAll();
    }

    public InstrumentationPermissionEntity findById(Long id) {
        return instrPermissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permissão de instrumentação não encontrada com ID: " + id));
    }

    public InstrumentationPermissionEntity findByUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        return instrPermissionRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Permissão de instrumentação não encontrada para o usuário"));
    }

    @Transactional
    public InstrumentationPermissionEntity createOrUpdate(InstrumentationPermissionDTO permissionDTO) {
        UserEntity user = userRepository.findById(permissionDTO.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + permissionDTO.getUserId()));

        InstrumentationPermissionEntity permission;

        var existingPermission = instrPermissionRepository.findByUser(user);

        if (existingPermission.isPresent()) {
            permission = existingPermission.get();
        } else {
            permission = new InstrumentationPermissionEntity();
            permission.setUser(user);
        }

        permission.setViewGraphs(permissionDTO.getViewGraphs());
        permission.setEditGraphsLocal(permissionDTO.getEditGraphsLocal());
        permission.setEditGraphsDefault(permissionDTO.getEditGraphsDefault());
        permission.setViewRead(permissionDTO.getViewRead());
        permission.setEditRead(permissionDTO.getEditRead());
        permission.setViewSections(permissionDTO.getViewSections());
        permission.setEditSections(permissionDTO.getEditSections());

        return instrPermissionRepository.save(permission);
    }

    @Transactional
    public void delete(Long id) {
        if (!instrPermissionRepository.existsById(id)) {
            throw new NotFoundException("Permissão de instrumentação não encontrada com ID: " + id);
        }
        instrPermissionRepository.deleteById(id);
    }

    @Transactional
    public void deleteByUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        if (!instrPermissionRepository.existsByUser(user)) {
            throw new NotFoundException("Permissão de instrumentação não encontrada para o usuário");
        }

        InstrumentationPermissionEntity permission = instrPermissionRepository.findByUser(user).get();
        permission.setUser(null);
        instrPermissionRepository.delete(permission);
    }

    @Transactional
    public void deleteByUserSafely(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

            if (instrPermissionRepository.existsByUser(user)) {
                InstrumentationPermissionEntity permission = instrPermissionRepository.findByUser(user).get();
                instrPermissionRepository.delete(permission);
            } else {
            }
        } catch (Exception e) {
            log.error("Error while trying to delete instrumentation permission for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    @Transactional
    public InstrumentationPermissionEntity createDefaultPermission(UserEntity user) {
        if (instrPermissionRepository.existsByUser(user)) {
            log.info("Instrumentation permission already exists for user {}", user.getId());
            return instrPermissionRepository.findByUser(user).get();
        }

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setUser(user);
        permission.setViewGraphs(false);
        permission.setEditGraphsLocal(false);
        permission.setEditGraphsDefault(false);
        permission.setViewRead(false);
        permission.setEditRead(false);
        permission.setViewSections(false);
        permission.setEditSections(false);

        InstrumentationPermissionEntity savedPermission = instrPermissionRepository.save(permission);
        return savedPermission;
    }
}
