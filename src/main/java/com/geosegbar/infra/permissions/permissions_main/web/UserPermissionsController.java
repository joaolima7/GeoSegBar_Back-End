package com.geosegbar.infra.permissions.permissions_main.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.permissions.permissions_main.dtos.UserPermissionsDTO;
import com.geosegbar.infra.permissions.permissions_main.dtos.UserPermissionsUpdateDTO;
import com.geosegbar.infra.permissions.permissions_main.services.UserPermissionsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user-permissions")
@RequiredArgsConstructor
public class UserPermissionsController {

    private final UserPermissionsService userPermissionsService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<WebResponseEntity<UserPermissionsDTO>> getUserPermissions(@PathVariable Long userId) {
        UserPermissionsDTO permissions = userPermissionsService.getAllPermissionsForUser(userId);
        WebResponseEntity<UserPermissionsDTO> response = 
            WebResponseEntity.success(permissions, "Permissões do usuário obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<WebResponseEntity<UserPermissionsDTO>> updateUserPermissions(
            @Valid @RequestBody UserPermissionsUpdateDTO updateDTO) {
        UserPermissionsDTO updatedPermissions = userPermissionsService.updateUserPermissions(updateDTO);
        WebResponseEntity<UserPermissionsDTO> response = 
            WebResponseEntity.success(updatedPermissions, "Permissões do usuário atualizadas com sucesso!");
        return ResponseEntity.ok(response);
    }
}
