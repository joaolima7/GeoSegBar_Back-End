package com.geosegbar.infra.security_level.web;

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
import com.geosegbar.entities.SecurityLevelEntity;
import com.geosegbar.infra.security_level.services.SecurityLevelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/security-levels")
@RequiredArgsConstructor
public class SecurityLevelController {
 
    private final SecurityLevelService securityLevelService;
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<SecurityLevelEntity>>> getAllSecurityLevels() {
        List<SecurityLevelEntity> securityLevels = securityLevelService.findAll();
        WebResponseEntity<List<SecurityLevelEntity>> response = WebResponseEntity.success(securityLevels, "Níveis de segurança obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<SecurityLevelEntity>> getSecurityLevelById(@PathVariable Long id) {
        SecurityLevelEntity securityLevel = securityLevelService.findById(id);
        WebResponseEntity<SecurityLevelEntity> response = WebResponseEntity.success(securityLevel, "Nível de segurança obtido com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<SecurityLevelEntity>> createSecurityLevel(@Valid @RequestBody SecurityLevelEntity securityLevel) {
        SecurityLevelEntity createdSecurityLevel = securityLevelService.save(securityLevel);
        WebResponseEntity<SecurityLevelEntity> response = WebResponseEntity.success(createdSecurityLevel, "Nível de segurança criado com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<SecurityLevelEntity>> updateSecurityLevel(@PathVariable Long id, @Valid @RequestBody SecurityLevelEntity securityLevel) {
        securityLevel.setId(id);
        SecurityLevelEntity updatedSecurityLevel = securityLevelService.update(securityLevel);
        WebResponseEntity<SecurityLevelEntity> response = WebResponseEntity.success(updatedSecurityLevel, "Nível de segurança atualizado com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteSecurityLevel(@PathVariable Long id) {
        securityLevelService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Nível de segurança excluído com sucesso!");
        return ResponseEntity.ok(response);
    }
}