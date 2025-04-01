package com.geosegbar.infra.documentation_permission.web;

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
import com.geosegbar.entities.DocumentationPermissionEntity;
import com.geosegbar.infra.documentation_permission.dtos.DocumentationPermissionDTO;
import com.geosegbar.infra.documentation_permission.services.DocumentationPermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/documentation-permission")
@RequiredArgsConstructor
public class DocumentationPermissionController {

    private final DocumentationPermissionService docPermissionService;
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<DocumentationPermissionEntity>>> getAllDocumentationPermissions() {
        List<DocumentationPermissionEntity> permissions = docPermissionService.findAll();
        WebResponseEntity<List<DocumentationPermissionEntity>> response = 
            WebResponseEntity.success(permissions, "Permissões de documentação obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<DocumentationPermissionEntity>> getDocumentationPermissionById(@PathVariable Long id) {
        DocumentationPermissionEntity permission = docPermissionService.findById(id);
        WebResponseEntity<DocumentationPermissionEntity> response = 
            WebResponseEntity.success(permission, "Permissão de documentação obtida com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<WebResponseEntity<DocumentationPermissionEntity>> getDocumentationPermissionByUser(@PathVariable Long userId) {
        DocumentationPermissionEntity permission = docPermissionService.findByUser(userId);
        WebResponseEntity<DocumentationPermissionEntity> response = 
            WebResponseEntity.success(permission, "Permissão de documentação do usuário obtida com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<DocumentationPermissionEntity>> createDocumentationPermission(
            @Valid @RequestBody DocumentationPermissionDTO permissionDTO) {
        DocumentationPermissionEntity permission = docPermissionService.createOrUpdate(permissionDTO);
        WebResponseEntity<DocumentationPermissionEntity> response = 
            WebResponseEntity.success(permission, "Permissão de documentação criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<DocumentationPermissionEntity>> updateDocumentationPermission(
            @PathVariable Long id,
            @Valid @RequestBody DocumentationPermissionDTO permissionDTO) {
        // Ensure the ID in the DTO matches the path variable
        permissionDTO.setId(id);
        DocumentationPermissionEntity permission = docPermissionService.createOrUpdate(permissionDTO);
        WebResponseEntity<DocumentationPermissionEntity> response = 
            WebResponseEntity.success(permission, "Permissão de documentação atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteDocumentationPermission(@PathVariable Long id) {
        docPermissionService.delete(id);
        WebResponseEntity<Void> response = 
            WebResponseEntity.success(null, "Permissão de documentação excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<WebResponseEntity<Void>> deleteDocumentationPermissionByUser(@PathVariable Long userId) {
        docPermissionService.deleteByUser(userId);
        WebResponseEntity<Void> response = 
            WebResponseEntity.success(null, "Permissão de documentação do usuário excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}
