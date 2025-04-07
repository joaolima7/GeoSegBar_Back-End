package com.geosegbar.infra.risk_category.web;

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
import com.geosegbar.entities.RiskCategoryEntity;
import com.geosegbar.infra.risk_category.services.RiskCategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/risk-categories")
@RequiredArgsConstructor
public class RiskCategoryController {
 
    private final RiskCategoryService riskCategoryService;
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<RiskCategoryEntity>>> getAllRiskCategories() {
        List<RiskCategoryEntity> riskCategories = riskCategoryService.findAll();
        WebResponseEntity<List<RiskCategoryEntity>> response = WebResponseEntity.success(riskCategories, "Categorias de risco obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<RiskCategoryEntity>> getRiskCategoryById(@PathVariable Long id) {
        RiskCategoryEntity riskCategory = riskCategoryService.findById(id);
        WebResponseEntity<RiskCategoryEntity> response = WebResponseEntity.success(riskCategory, "Categoria de risco obtida com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<RiskCategoryEntity>> createRiskCategory(@Valid @RequestBody RiskCategoryEntity riskCategory) {
        RiskCategoryEntity createdRiskCategory = riskCategoryService.save(riskCategory);
        WebResponseEntity<RiskCategoryEntity> response = WebResponseEntity.success(createdRiskCategory, "Categoria de risco criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<RiskCategoryEntity>> updateRiskCategory(@PathVariable Long id, @Valid @RequestBody RiskCategoryEntity riskCategory) {
        riskCategory.setId(id);
        RiskCategoryEntity updatedRiskCategory = riskCategoryService.update(riskCategory);
        WebResponseEntity<RiskCategoryEntity> response = WebResponseEntity.success(updatedRiskCategory, "Categoria de risco atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteRiskCategory(@PathVariable Long id) {
        riskCategoryService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Categoria de risco excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}