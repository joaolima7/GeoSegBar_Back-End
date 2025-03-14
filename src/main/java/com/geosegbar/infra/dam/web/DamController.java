package com.geosegbar.infra.dam.web;

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
import com.geosegbar.entities.DamEntity;
import com.geosegbar.infra.dam.services.DamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dams")
@RequiredArgsConstructor
public class DamController {
 
    private final DamService damService;
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<DamEntity>>> getAllDams() {
        List<DamEntity> dams = damService.findAll();
        WebResponseEntity<List<DamEntity>> response = WebResponseEntity.success(dams, "Barragens obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<DamEntity>> getDamById(@PathVariable Long id) {
        DamEntity dam = damService.findById(id);
        WebResponseEntity<DamEntity> response = WebResponseEntity.success(dam, "Barragem obtida com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<DamEntity>> createDam(@Valid @RequestBody DamEntity dam) {
        DamEntity createdDam = damService.save(dam);
        WebResponseEntity<DamEntity> response = WebResponseEntity.success(createdDam, "Barragem criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<DamEntity>> updateDam(@PathVariable Long id, @Valid @RequestBody DamEntity dam) {
        dam.setId(id);
        DamEntity updatedDam = damService.update(dam);
        WebResponseEntity<DamEntity> response = WebResponseEntity.success(updatedDam, "Barragem atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteDam(@PathVariable Long id) {
        damService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Barragem excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}