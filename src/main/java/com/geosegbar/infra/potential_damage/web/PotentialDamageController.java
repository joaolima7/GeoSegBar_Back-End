package com.geosegbar.infra.potential_damage.web;

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
import com.geosegbar.entities.PotentialDamageEntity;
import com.geosegbar.infra.potential_damage.services.PotentialDamageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/potential-damages")
@RequiredArgsConstructor
public class PotentialDamageController {
 
    private final PotentialDamageService potentialDamageService;
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<PotentialDamageEntity>>> getAllPotentialDamages() {
        List<PotentialDamageEntity> potentialDamages = potentialDamageService.findAll();
        WebResponseEntity<List<PotentialDamageEntity>> response = WebResponseEntity.success(potentialDamages, "Danos potenciais obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<PotentialDamageEntity>> getPotentialDamageById(@PathVariable Long id) {
        PotentialDamageEntity potentialDamage = potentialDamageService.findById(id);
        WebResponseEntity<PotentialDamageEntity> response = WebResponseEntity.success(potentialDamage, "Dano potencial obtido com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<PotentialDamageEntity>> createPotentialDamage(@Valid @RequestBody PotentialDamageEntity potentialDamage) {
        PotentialDamageEntity createdPotentialDamage = potentialDamageService.save(potentialDamage);
        WebResponseEntity<PotentialDamageEntity> response = WebResponseEntity.success(createdPotentialDamage, "Dano potencial criado com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<PotentialDamageEntity>> updatePotentialDamage(@PathVariable Long id, @Valid @RequestBody PotentialDamageEntity potentialDamage) {
        potentialDamage.setId(id);
        PotentialDamageEntity updatedPotentialDamage = potentialDamageService.update(potentialDamage);
        WebResponseEntity<PotentialDamageEntity> response = WebResponseEntity.success(updatedPotentialDamage, "Dano potencial atualizado com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deletePotentialDamage(@PathVariable Long id) {
        potentialDamageService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Dano potencial excluído com sucesso!");
        return ResponseEntity.ok(response);
    }
}