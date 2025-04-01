package com.geosegbar.infra.permissions.permissions_main.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.AttributionsPermissionEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.DocumentationPermissionEntity;
import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.RoutineInspectionPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.permissions.atributions_permission.persistence.AttributionsPermissionRepository;
import com.geosegbar.infra.permissions.dam_permissions.persistence.DamPermissionRepository;
import com.geosegbar.infra.permissions.documentation_permission.persistence.DocumentationPermissionRepository;
import com.geosegbar.infra.permissions.instrumentation_permission.persistence.InstrumentationPermissionRepository;
import com.geosegbar.infra.permissions.permissions_main.dtos.UserPermissionsDTO;
import com.geosegbar.infra.permissions.routine_inspection_permission.persistence.RoutineInspectionPermissionRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

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
}
