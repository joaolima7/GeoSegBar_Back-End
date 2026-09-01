package com.geosegbar.infra.permissions.permissions_main.web;

import com.geosegbar.infra.permissions.permissions_main.dtos.VerifyChecklistsDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        WebResponseEntity<UserPermissionsDTO> response
                = WebResponseEntity.success(permissions, "Permissões do usuário obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<WebResponseEntity<UserPermissionsDTO>> updateUserPermissions(
            @Valid @RequestBody UserPermissionsUpdateDTO updateDTO) {
        UserPermissionsDTO updatedPermissions = userPermissionsService.updateUserPermissions(updateDTO);
        WebResponseEntity<UserPermissionsDTO> response
                = WebResponseEntity.success(updatedPermissions, "Permissões do usuário atualizadas com sucesso!");
        return ResponseEntity.ok(response);
    }

    /**
     * Todos os checklists da barragem numa resposta, com código de motivo.
     * A rota unitária acima fica intacta.
     */
    @GetMapping("/verify-checklists")
    public ResponseEntity<WebResponseEntity<VerifyChecklistsDTO>> verifyChecklistsForDam(
            @RequestParam Long userId,
            @RequestParam Long clientId,
            @RequestParam Long damId,
            @RequestParam boolean isMobile) {

        VerifyChecklistsDTO resultado = userPermissionsService.verifyChecklistsForDam(
                userId, clientId, damId, isMobile);

        return ResponseEntity.ok(WebResponseEntity.success(
                resultado, "Permissões de checklist da barragem obtidas com sucesso!"));
    }

    @GetMapping("/verify-checklist")
    public ResponseEntity<WebResponseEntity<Object>> verifyChecklistPermission(
            @RequestParam Long userId,
            @RequestParam Long clientId,
            @RequestParam Long damId,
            @RequestParam Long checklistId,
            @RequestParam boolean isMobile) {

        String result = userPermissionsService.verifyChecklistPermission(userId, clientId, damId, checklistId, isMobile);

        if ("authorized".equals(result)) {
            WebResponseEntity<Object> response = WebResponseEntity.success(
                    true,
                    "Usuário autorizado a preencher o checklist"
            );
            return ResponseEntity.ok(response);
        } else {
            WebResponseEntity<Object> response = WebResponseEntity.error(
                    result
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }
}
