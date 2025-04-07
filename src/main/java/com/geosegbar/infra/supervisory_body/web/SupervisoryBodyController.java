package com.geosegbar.infra.supervisory_body.web;

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
import com.geosegbar.entities.SupervisoryBodyEntity;
import com.geosegbar.infra.supervisory_body.services.SupervisoryBodyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/supervisory-bodies")
@RequiredArgsConstructor
public class SupervisoryBodyController {
 
    private final SupervisoryBodyService supervisoryBodyService;
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<SupervisoryBodyEntity>>> getAllSupervisoryBodies() {
        List<SupervisoryBodyEntity> supervisoryBodies = supervisoryBodyService.findAll();
        WebResponseEntity<List<SupervisoryBodyEntity>> response = WebResponseEntity.success(supervisoryBodies, "Órgãos fiscalizadores obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<SupervisoryBodyEntity>> getSupervisoryBodyById(@PathVariable Long id) {
        SupervisoryBodyEntity supervisoryBody = supervisoryBodyService.findById(id);
        WebResponseEntity<SupervisoryBodyEntity> response = WebResponseEntity.success(supervisoryBody, "Órgão fiscalizador obtido com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<SupervisoryBodyEntity>> createSupervisoryBody(@Valid @RequestBody SupervisoryBodyEntity supervisoryBody) {
        SupervisoryBodyEntity createdSupervisoryBody = supervisoryBodyService.save(supervisoryBody);
        WebResponseEntity<SupervisoryBodyEntity> response = WebResponseEntity.success(createdSupervisoryBody, "Órgão fiscalizador criado com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<SupervisoryBodyEntity>> updateSupervisoryBody(@PathVariable Long id, @Valid @RequestBody SupervisoryBodyEntity supervisoryBody) {
        supervisoryBody.setId(id);
        SupervisoryBodyEntity updatedSupervisoryBody = supervisoryBodyService.update(supervisoryBody);
        WebResponseEntity<SupervisoryBodyEntity> response = WebResponseEntity.success(updatedSupervisoryBody, "Órgão fiscalizador atualizado com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteSupervisoryBody(@PathVariable Long id) {
        supervisoryBodyService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Órgão fiscalizador excluído com sucesso!");
        return ResponseEntity.ok(response);
    }
}