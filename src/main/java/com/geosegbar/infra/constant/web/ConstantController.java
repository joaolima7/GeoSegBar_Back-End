package com.geosegbar.infra.constant.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.infra.constant.services.ConstantService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/constants")
@RequiredArgsConstructor
public class ConstantController {

    private final ConstantService constantService;

    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<List<ConstantEntity>>> getConstantsByInstrument(@PathVariable Long instrumentId) {
        List<ConstantEntity> constants = constantService.findByInstrumentId(instrumentId);
        return ResponseEntity.ok(WebResponseEntity.success(constants, "Constantes do instrumento obtidas com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ConstantEntity>> getConstantById(@PathVariable Long id) {
        ConstantEntity constant = constantService.findById(id);
        return ResponseEntity.ok(WebResponseEntity.success(constant, "Constante obtida com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteConstant(@PathVariable Long id) {
        constantService.deleteById(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Constante excluída com sucesso!"));
    }
}
