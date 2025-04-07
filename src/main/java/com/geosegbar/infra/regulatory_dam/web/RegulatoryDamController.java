package com.geosegbar.infra.regulatory_dam.web;

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
import com.geosegbar.entities.RegulatoryDamEntity;
import com.geosegbar.infra.regulatory_dam.dtos.RegulatoryDamDTO;
import com.geosegbar.infra.regulatory_dam.services.RegulatoryDamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/regulatory-dam")
@RequiredArgsConstructor
public class RegulatoryDamController {

    private final RegulatoryDamService regulatoryDamService;
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<RegulatoryDamEntity>>> getAllRegulatoryDams() {
        List<RegulatoryDamEntity> regulatoryDams = regulatoryDamService.findAll();
        WebResponseEntity<List<RegulatoryDamEntity>> response = 
            WebResponseEntity.success(regulatoryDams, "Informações regulatórias das barragens obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<RegulatoryDamEntity>> getRegulatoryDamById(@PathVariable Long id) {
        RegulatoryDamEntity regulatoryDam = regulatoryDamService.findById(id);
        WebResponseEntity<RegulatoryDamEntity> response = 
            WebResponseEntity.success(regulatoryDam, "Informação regulatória da barragem obtida com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<RegulatoryDamEntity>> getRegulatoryDamByDamId(@PathVariable Long damId) {
        RegulatoryDamEntity regulatoryDam = regulatoryDamService.findByDamId(damId);
        WebResponseEntity<RegulatoryDamEntity> response = 
            WebResponseEntity.success(regulatoryDam, "Informação regulatória da barragem obtida com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<RegulatoryDamEntity>> createRegulatoryDam(
            @Valid @RequestBody RegulatoryDamDTO regulatoryDamDTO) {
        RegulatoryDamEntity createdRegulatoryDam = regulatoryDamService.createOrUpdate(regulatoryDamDTO);
        WebResponseEntity<RegulatoryDamEntity> response = 
            WebResponseEntity.success(createdRegulatoryDam, "Informação regulatória da barragem criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<RegulatoryDamEntity>> updateRegulatoryDam(
            @PathVariable Long id,
            @Valid @RequestBody RegulatoryDamDTO regulatoryDamDTO) {
        regulatoryDamDTO.setId(id);
        RegulatoryDamEntity updatedRegulatoryDam = regulatoryDamService.createOrUpdate(regulatoryDamDTO);
        WebResponseEntity<RegulatoryDamEntity> response = 
            WebResponseEntity.success(updatedRegulatoryDam, "Informação regulatória da barragem atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteRegulatoryDam(@PathVariable Long id) {
        regulatoryDamService.delete(id);
        WebResponseEntity<Void> response = 
            WebResponseEntity.success(null, "Informação regulatória da barragem excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}