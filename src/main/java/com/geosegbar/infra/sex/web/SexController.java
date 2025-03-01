package com.geosegbar.infra.sex.web;

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

import com.geosegbar.common.WebResponseEntity;
import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.infra.sex.services.SexService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("sex")
@RequiredArgsConstructor
public class SexController {
    
    private final SexService sexService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<SexEntity>>> getAllSexs() {
        List<SexEntity> sexs = sexService.findAll();
        WebResponseEntity<List<SexEntity>> response = WebResponseEntity.success(sexs, "Sexos obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<SexEntity>> getSexById(@PathVariable Long id) {
        SexEntity sex = sexService.findById(id);
        WebResponseEntity<SexEntity> response = WebResponseEntity.success(sex, "Sexo obtido com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<SexEntity>> createSex(@Valid @RequestBody SexEntity sex) {
        SexEntity createdSex = sexService.save(sex);
        WebResponseEntity<SexEntity> response = WebResponseEntity.success(createdSex, "Sexo criado com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<SexEntity>> updateDam(@PathVariable Long id, @Valid @RequestBody SexEntity sex) {
        sex.setId(id);
        SexEntity updatedSex = sexService.save(sex);
        WebResponseEntity<SexEntity> response = WebResponseEntity.success(updatedSex, "Sexo atualizado com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteSex(@PathVariable Long id) {
        sexService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Sexo excluído com sucesso!");
        return ResponseEntity.ok(response);
    }
}
