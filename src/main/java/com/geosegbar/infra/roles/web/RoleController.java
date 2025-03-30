package com.geosegbar.infra.roles.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.RoleEntity;
import com.geosegbar.infra.roles.services.RoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<RoleEntity>>> getAllRoles() {
        List<RoleEntity> roles = roleService.findAll();
        WebResponseEntity<List<RoleEntity>> response = WebResponseEntity.success(roles, "Roles obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }
}
