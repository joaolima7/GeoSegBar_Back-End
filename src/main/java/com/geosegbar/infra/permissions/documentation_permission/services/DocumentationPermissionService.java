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
    
    public List<DocumentationPermissionEntity> findAll() {
        return docPermissionRepository.findAll();
    }
    
    public DocumentationPermissionEntity findById(Long id) {
        return docPermissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permissão de documentação não encontrada com ID: " + id));
    }
    
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
        
        DocumentationPermissionEntity permission;
        
        var existingPermission = docPermissionRepository.findByUser(user);
        
        if (existingPermission.isPresent()) {
            permission = existingPermission.get();
        } else {
            permission = new DocumentationPermissionEntity();
            permission.setUser(user);
        }
        
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
            
            if (docPermissionRepository.existsByUser(user)) {
                docPermissionRepository.deleteByUser(user);
            } 
        } catch (Exception e) {
            log.warn("Error while trying to delete documentation permission for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional
    public DocumentationPermissionEntity createDefaultPermission(UserEntity user) {        
        if (docPermissionRepository.existsByUser(user)) {
            log.info("Documentation permission already exists for user {}", user.getId());
            return docPermissionRepository.findByUser(user).get();
        }
        
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setUser(user);
        permission.setViewPSB(false);
        permission.setEditPSB(false);
        permission.setSharePSB(false);
        
        DocumentationPermissionEntity savedPermission = docPermissionRepository.save(permission);
        return savedPermission;
    }
}