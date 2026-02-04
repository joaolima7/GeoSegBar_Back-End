package com.geosegbar.infra.permissions.documentation_permission.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.DocumentationPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.permissions.documentation_permission.dtos.DocumentationPermissionDTO;
import com.geosegbar.infra.permissions.documentation_permission.persistence.DocumentationPermissionRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationPermissionService {

    private final DocumentationPermissionRepository docPermissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DocumentationPermissionEntity> findAll() {
        return docPermissionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DocumentationPermissionEntity findById(Long id) {
        return docPermissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permissão de documentação não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public DocumentationPermissionEntity findByUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        return docPermissionRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Permissão de documentação não encontrada para o usuário"));
    }

    @Transactional
    public DocumentationPermissionEntity createOrUpdate(DocumentationPermissionDTO permissionDTO) {
        UserEntity user = userRepository.findById(permissionDTO.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + permissionDTO.getUserId()));

        DocumentationPermissionEntity permission = docPermissionRepository.findByUser(user)
                .orElseGet(() -> {
                    DocumentationPermissionEntity newPerm = new DocumentationPermissionEntity();
                    newPerm.setUser(user);
                    return newPerm;
                });

        permission.setViewPSB(permissionDTO.getViewPSB());
        permission.setEditPSB(permissionDTO.getEditPSB());
        permission.setSharePSB(permissionDTO.getSharePSB());

        return docPermissionRepository.save(permission);
    }

    @Transactional
    public void delete(Long id) {
        if (!docPermissionRepository.existsById(id)) {
            throw new NotFoundException("Permissão de documentação não encontrada com ID: " + id);
        }
        docPermissionRepository.deleteById(id);
    }

    @Transactional
    public void deleteByUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        if (!docPermissionRepository.existsByUser(user)) {
            throw new NotFoundException("Permissão de documentação não encontrada para o usuário");
        }

        docPermissionRepository.deleteByUser(user);
    }

    @Transactional
    public void deleteByUserSafely(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

            docPermissionRepository.findByUser(user).ifPresent(permission -> {
                user.setDocumentationPermission(null);
                permission.setUser(null);

                userRepository.save(user);
                docPermissionRepository.delete(permission);
            });
        } catch (Exception e) {
            log.warn("Error while trying to delete documentation permission for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional
    public DocumentationPermissionEntity createDefaultPermission(UserEntity user) {
        return docPermissionRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info("Creating default documentation permission for user {}", user.getId());
                    DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
                    permission.setUser(user);
                    permission.setViewPSB(false);
                    permission.setEditPSB(false);
                    permission.setSharePSB(false);
                    return docPermissionRepository.save(permission);
                });
    }
}
