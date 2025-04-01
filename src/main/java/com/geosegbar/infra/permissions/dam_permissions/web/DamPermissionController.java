package com.geosegbar.infra.permissions.dam_permissions.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.infra.permissions.dam_permissions.dtos.UserDamPermissionsRequestDTO;
import com.geosegbar.infra.permissions.dam_permissions.services.DamPermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dam-permission")
@RequiredArgsConstructor
public class DamPermissionController {

    private final DamPermissionService damPermissionService;
    
    @GetMapping("/user/{userId}/all-client-dams")
    public ResponseEntity<WebResponseEntity<List<DamPermissionEntity>>> getAllDamPermissionsForUserClients(@PathVariable Long userId) {
        List<DamPermissionEntity> permissions = damPermissionService.findAllDamPermissionsForUserClients(userId);
        WebResponseEntity<List<DamPermissionEntity>> response = 
            WebResponseEntity.success(permissions, "Permissões de todas as barragens dos clientes do usuário obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-access/{userId}/{damId}")
    public ResponseEntity<WebResponseEntity<Boolean>> checkUserHasAccessToDam(
            @PathVariable Long userId, @PathVariable Long damId) {
        boolean hasAccess = damPermissionService.checkUserHasAccessToDam(userId, damId);
        WebResponseEntity<Boolean> response = 
            WebResponseEntity.success(hasAccess, "Verificação de acesso realizada com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/setup-for-user/{userId}")
    public ResponseEntity<WebResponseEntity<List<DamPermissionEntity>>> setupPermissionsForUser(
            @PathVariable Long userId, 
            @Valid @RequestBody UserDamPermissionsRequestDTO requestDTO) {
        List<DamPermissionEntity> permissions = damPermissionService.setupPermissionsForUser(userId, requestDTO);
        WebResponseEntity<List<DamPermissionEntity>> response = 
            WebResponseEntity.success(permissions, "Permissões de barragem configuradas com sucesso para o usuário!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/update-for-user/{userId}")
    public ResponseEntity<WebResponseEntity<List<DamPermissionEntity>>> updatePermissionsForUser(
            @PathVariable Long userId, 
            @Valid @RequestBody UserDamPermissionsRequestDTO requestDTO) {
        List<DamPermissionEntity> permissions = damPermissionService.updatePermissionsForUser(userId, requestDTO);
        WebResponseEntity<List<DamPermissionEntity>> response = 
            WebResponseEntity.success(permissions, "Permissões de barragem atualizadas com sucesso para o usuário!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteDamPermission(@PathVariable Long id) {
        damPermissionService.delete(id);
        WebResponseEntity<Void> response = 
            WebResponseEntity.success(null, "Permissão de barragem excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}