package com.geosegbar.infra.permissions.permissions_main.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.AttributionsPermissionEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.DocumentationPermissionEntity;
import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.RoutineInspectionPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.permissions.atributions_permission.persistence.AttributionsPermissionRepository;
import com.geosegbar.infra.permissions.dam_permissions.persistence.DamPermissionRepository;
import com.geosegbar.infra.permissions.documentation_permission.persistence.DocumentationPermissionRepository;
import com.geosegbar.infra.permissions.instrumentation_permission.persistence.InstrumentationPermissionRepository;
import com.geosegbar.infra.permissions.permissions_main.dtos.UserPermissionsDTO;
import com.geosegbar.infra.permissions.permissions_main.dtos.UserPermissionsUpdateDTO;
import com.geosegbar.infra.permissions.permissions_main.dtos.UserPermissionsUpdateDTO.AttributionsPermissionUpdateDTO;
import com.geosegbar.infra.permissions.permissions_main.dtos.UserPermissionsUpdateDTO.DocumentationPermissionUpdateDTO;
import com.geosegbar.infra.permissions.permissions_main.dtos.UserPermissionsUpdateDTO.InstrumentationPermissionUpdateDTO;
import com.geosegbar.infra.permissions.permissions_main.dtos.UserPermissionsUpdateDTO.RoutineInspectionPermissionUpdateDTO;
import com.geosegbar.infra.permissions.routine_inspection_permission.persistence.RoutineInspectionPermissionRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPermissionsService {

    private final UserRepository userRepository;
    private final DocumentationPermissionRepository documentationPermissionRepository;
    private final AttributionsPermissionRepository attributionsPermissionRepository;
    private final InstrumentationPermissionRepository instrumentationPermissionRepository;
    private final RoutineInspectionPermissionRepository routineInspectionPermissionRepository;
    private final DamPermissionRepository damPermissionRepository;
    private final DamRepository damRepository;

    public UserPermissionsDTO getAllPermissionsForUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        UserPermissionsDTO permissionsDTO = new UserPermissionsDTO();

        Optional<DocumentationPermissionEntity> docPermission = documentationPermissionRepository.findByUser(user);
        permissionsDTO.setDocumentationPermission(docPermission.orElse(null));

        Optional<AttributionsPermissionEntity> attrPermission = attributionsPermissionRepository.findByUser(user);
        permissionsDTO.setAttributionsPermission(attrPermission.orElse(null));

        Optional<InstrumentationPermissionEntity> instrPermission = instrumentationPermissionRepository.findByUser(user);
        permissionsDTO.setInstrumentationPermission(instrPermission.orElse(null));

        Optional<RoutineInspectionPermissionEntity> routinePermission = routineInspectionPermissionRepository.findByUser(user);
        permissionsDTO.setRoutineInspectionPermission(routinePermission.orElse(null));

        List<DamPermissionEntity> damPermissions = damPermissionRepository.findByUser(user);
        permissionsDTO.setDamPermissions(damPermissions);

        return permissionsDTO;
    }

    @Transactional
public UserPermissionsDTO updateUserPermissions(UserPermissionsUpdateDTO updateDTO) {
    UserEntity user = userRepository.findById(updateDTO.getUserId())
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + updateDTO.getUserId()));
    
    if (updateDTO.getDocumentationPermission() != null) {
        updateDocumentationPermission(user, updateDTO.getDocumentationPermission());
    }
    
    if (updateDTO.getAttributionsPermission() != null) {
        updateAttributionsPermission(user, updateDTO.getAttributionsPermission());
    }
    
    if (updateDTO.getInstrumentationPermission() != null) {
        updateInstrumentationPermission(user, updateDTO.getInstrumentationPermission());
    }
    
    if (updateDTO.getRoutineInspectionPermission() != null) {
        updateRoutineInspectionPermission(user, updateDTO.getRoutineInspectionPermission());
    }
    
    if (updateDTO.getDamIds() != null) {
        updateDamPermissions(user, updateDTO.getDamIds());
    }
    
    return getAllPermissionsForUser(user.getId());
}

private void updateDocumentationPermission(UserEntity user, DocumentationPermissionUpdateDTO updateDTO) {
    DocumentationPermissionEntity permission;
    Optional<DocumentationPermissionEntity> existingPermission = documentationPermissionRepository.findByUser(user);
    
    if (existingPermission.isPresent()) {
        permission = existingPermission.get();
    } else {
        permission = new DocumentationPermissionEntity();
        permission.setUser(user);
    }
    
    if (updateDTO.getViewPSB() != null) {
        permission.setViewPSB(updateDTO.getViewPSB());
    }
    if (updateDTO.getEditPSB() != null) {
        permission.setEditPSB(updateDTO.getEditPSB());
    }
    if (updateDTO.getSharePSB() != null) {
        permission.setSharePSB(updateDTO.getSharePSB());
    }
    
    documentationPermissionRepository.save(permission);
}

private void updateAttributionsPermission(UserEntity user, AttributionsPermissionUpdateDTO updateDTO) {
    AttributionsPermissionEntity permission;
    Optional<AttributionsPermissionEntity> existingPermission = attributionsPermissionRepository.findByUser(user);
    
    if (existingPermission.isPresent()) {
        permission = existingPermission.get();
    } else {
        permission = new AttributionsPermissionEntity();
        permission.setUser(user);
    }
    
    if (updateDTO.getEditUser() != null) {
        permission.setEditUser(updateDTO.getEditUser());
    }
    
    if (updateDTO.getEditDam() != null) {
        permission.setEditDam(updateDTO.getEditDam());
    }
    
    if (updateDTO.getEditGeralData() != null) {
        permission.setEditGeralData(updateDTO.getEditGeralData());
    }
    
    attributionsPermissionRepository.save(permission);
}

private void updateInstrumentationPermission(UserEntity user, InstrumentationPermissionUpdateDTO updateDTO) {
    InstrumentationPermissionEntity permission;
    Optional<InstrumentationPermissionEntity> existingPermission = instrumentationPermissionRepository.findByUser(user);
    
    if (existingPermission.isPresent()) {
        permission = existingPermission.get();
    } else {
        permission = new InstrumentationPermissionEntity();
        permission.setUser(user);
    }
    
    if (updateDTO.getViewGraphs() != null) {
        permission.setViewGraphs(updateDTO.getViewGraphs());
    }
    
    if (updateDTO.getEditGraphsLocal() != null) {
        permission.setEditGraphsLocal(updateDTO.getEditGraphsLocal());
    }
    
    if (updateDTO.getEditGraphsDefault() != null) {
        permission.setEditGraphsDefault(updateDTO.getEditGraphsDefault());
    }
    
    if (updateDTO.getViewRead() != null) {
        permission.setViewRead(updateDTO.getViewRead());
    }
    
    if (updateDTO.getEditRead() != null) {
        permission.setEditRead(updateDTO.getEditRead());
    }
    
    if (updateDTO.getViewSections() != null) {
        permission.setViewSections(updateDTO.getViewSections());
    }
    
    if (updateDTO.getEditSections() != null) {
        permission.setEditSections(updateDTO.getEditSections());
    }
    
    instrumentationPermissionRepository.save(permission);
    log.info("Updated instrumentation permission for user {}", user.getId());
}

private void updateRoutineInspectionPermission(UserEntity user, RoutineInspectionPermissionUpdateDTO updateDTO) {
    RoutineInspectionPermissionEntity permission;
    Optional<RoutineInspectionPermissionEntity> existingPermission = routineInspectionPermissionRepository.findByUser(user);
    
    if (existingPermission.isPresent()) {
        permission = existingPermission.get();
    } else {
        permission = new RoutineInspectionPermissionEntity();
        permission.setUser(user);
    }
    
    if (updateDTO.getIsFillWeb() != null) {
        permission.setIsFillWeb(updateDTO.getIsFillWeb());
    }
    
    if (updateDTO.getIsFillMobile() != null) {
        permission.setIsFillMobile(updateDTO.getIsFillMobile());
    }
    
    routineInspectionPermissionRepository.save(permission);
}

private void updateDamPermissions(UserEntity user, List<Long> damIds) {
    try {
        List<DamPermissionEntity> existingPermissions = damPermissionRepository.findByUser(user);
        
        for (DamPermissionEntity existing : existingPermissions) {
            if (!damIds.contains(existing.getDam().getId())) {
                damPermissionRepository.delete(existing);
            }
        }
        
        existingPermissions = damPermissionRepository.findByUser(user);
        
        List<Long> existingDamIds = existingPermissions.stream()
                .map(perm -> perm.getDam().getId())
                .toList();
        
        for (Long damId : damIds) {
            if (existingDamIds.contains(damId)) {
                continue;
            }
            
            DamEntity dam = damRepository.findById(damId)
                    .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + damId));
            
            DamPermissionEntity permission = new DamPermissionEntity();
            permission.setUser(user);
            permission.setDam(dam);
            permission.setClient(dam.getClient());
            permission.setHasAccess(false);
            permission.setCreatedAt(LocalDateTime.now());
            
            damPermissionRepository.save(permission);
        }
        
    } catch (Exception e) {
        log.error("Error updating dam permissions for user {}: {}", user.getId(), e.getMessage(), e);
        throw e;
    }
}
}
