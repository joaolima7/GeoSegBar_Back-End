package com.geosegbar.infra.classification_dam.web;

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
import com.geosegbar.entities.ClassificationDamEntity;
import com.geosegbar.infra.classification_dam.services.ClassificationDamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/classification-dams")
@RequiredArgsConstructor
public class ClassificationDamController {
 
    private final ClassificationDamService classificationDamService;
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<ClassificationDamEntity>>> getAllClassificationDams() {
        List<ClassificationDamEntity> classificationDams = classificationDamService.findAll();
        WebResponseEntity<List<ClassificationDamEntity>> response = WebResponseEntity.success(classificationDams, "Classificações de barragem obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ClassificationDamEntity>> getClassificationDamById(@PathVariable Long id) {
        ClassificationDamEntity classificationDam = classificationDamService.findById(id);
        WebResponseEntity<ClassificationDamEntity> response = WebResponseEntity.success(classificationDam, "Classificação de barragem obtida com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<ClassificationDamEntity>> createClassificationDam(@Valid @RequestBody ClassificationDamEntity classificationDam) {
        ClassificationDamEntity createdClassificationDam = classificationDamService.save(classificationDam);
        WebResponseEntity<ClassificationDamEntity> response = WebResponseEntity.success(createdClassificationDam, "Classificação de barragem criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ClassificationDamEntity>> updateClassificationDam(@PathVariable Long id, @Valid @RequestBody ClassificationDamEntity classificationDam) {
        classificationDam.setId(id);
        ClassificationDamEntity updatedClassificationDam = classificationDamService.update(classificationDam);
        WebResponseEntity<ClassificationDamEntity> response = WebResponseEntity.success(updatedClassificationDam, "Classificação de barragem atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteClassificationDam(@PathVariable Long id) {
        classificationDamService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Classificação de barragem excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}