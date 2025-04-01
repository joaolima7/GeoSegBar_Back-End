package com.geosegbar.infra.routine_inspection_permission.services;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.RoutineInspectionPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.routine_inspection_permission.dtos.RoutineInspectionPermissionDTO;
import com.geosegbar.infra.routine_inspection_permission.persistence.RoutineInspectionPermissionRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutineInspectionPermissionService {

    private final RoutineInspectionPermissionRepository routinePermissionRepository;
    private final UserRepository userRepository;
    
    public List<RoutineInspectionPermissionEntity> findAll() {
        return routinePermissionRepository.findAll();
    }
    
    public RoutineInspectionPermissionEntity findById(Long id) {
        return routinePermissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permissão de inspeção de rotina não encontrada com ID: " + id));
    }
    
    public RoutineInspectionPermissionEntity findByUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));
                
        return routinePermissionRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Permissão de inspeção de rotina não encontrada para o usuário"));
    }
    
    @Transactional
    public RoutineInspectionPermissionEntity createOrUpdate(RoutineInspectionPermissionDTO permissionDTO) {
        UserEntity user = userRepository.findById(permissionDTO.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + permissionDTO.getUserId()));
        
        RoutineInspectionPermissionEntity permission;
        
        var existingPermission = routinePermissionRepository.findByUser(user);
        
        if (existingPermission.isPresent()) {
            permission = existingPermission.get();
        } else {
            permission = new RoutineInspectionPermissionEntity();
            permission.setUser(user);
        }
        
        permission.setIsFillWeb(permissionDTO.getIsFillWeb());
        permission.setIsFillMobile(permissionDTO.getIsFillMobile());
        
        return routinePermissionRepository.save(permission);
    }
    
    @Transactional
    public void delete(Long id) {
        if (!routinePermissionRepository.existsById(id)) {
            throw new NotFoundException("Permissão de inspeção de rotina não encontrada com ID: " + id);
        }
        routinePermissionRepository.deleteById(id);
    }
    
    @Transactional
    public void deleteByUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));
                
        if (!routinePermissionRepository.existsByUser(user)) {
            throw new NotFoundException("Permissão de inspeção de rotina não encontrada para o usuário");
        }
        
        RoutineInspectionPermissionEntity permission = routinePermissionRepository.findByUser(user).get();
        permission.setUser(null);
        routinePermissionRepository.delete(permission);
    }
    
    @Transactional
    public void deleteByUserSafely(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));
            
            if (routinePermissionRepository.existsByUser(user)) {
                RoutineInspectionPermissionEntity permission = routinePermissionRepository.findByUser(user).get();
                permission.setUser(null);
                routinePermissionRepository.delete(permission);
            } else {
                log.info("No routine inspection permission found for user {}", userId);
            }
        } catch (Exception e) {
            log.error("Error while trying to delete routine inspection permission for user {}: {}", 
                userId, e.getMessage(), e);
        }
    }

    @Transactional
    public RoutineInspectionPermissionEntity createDefaultPermission(UserEntity user) {        
        if (routinePermissionRepository.existsByUser(user)) {
            return routinePermissionRepository.findByUser(user).get();
        }
        
        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setUser(user);
        permission.setIsFillWeb(false);
        permission.setIsFillMobile(false);
        
        RoutineInspectionPermissionEntity savedPermission = routinePermissionRepository.save(permission);
        return savedPermission;
    }
}
