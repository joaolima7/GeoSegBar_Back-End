package com.geosegbar.infra.statistical_limit.web;

import java.util.Optional;

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
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.infra.statistical_limit.services.StatisticalLimitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/statistical-limits")
@RequiredArgsConstructor
public class StatisticalLimitController {

    private final StatisticalLimitService statisticalLimitService;

    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<StatisticalLimitEntity>> getByInstrumentId(@PathVariable Long instrumentId) {
        Optional<StatisticalLimitEntity> limit = statisticalLimitService.findByInstrumentId(instrumentId);
        return limit.map(l -> ResponseEntity.ok(WebResponseEntity.success(l, "Limite estatístico obtido com sucesso!")))
                .orElseGet(() -> ResponseEntity.ok(WebResponseEntity.success(null, "Instrumento não possui limite estatístico")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<StatisticalLimitEntity>> getById(@PathVariable Long id) {
        StatisticalLimitEntity limit = statisticalLimitService.findById(id);
        return ResponseEntity.ok(WebResponseEntity.success(limit, "Limite estatístico obtido com sucesso!"));
    }

    @PostMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<StatisticalLimitEntity>> create(
            @PathVariable Long instrumentId,
            @Valid @RequestBody StatisticalLimitEntity limit) {
        StatisticalLimitEntity created = statisticalLimitService.createOrUpdate(instrumentId, limit);
        return new ResponseEntity<>(WebResponseEntity.success(created, "Limite estatístico criado com sucesso!"), HttpStatus.CREATED);
    }

    @PutMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<StatisticalLimitEntity>> update(
            @PathVariable Long instrumentId,
            @Valid @RequestBody StatisticalLimitEntity limit) {
        StatisticalLimitEntity updated = statisticalLimitService.createOrUpdate(instrumentId, limit);
        return ResponseEntity.ok(WebResponseEntity.success(updated, "Limite estatístico atualizado com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> delete(@PathVariable Long id) {
        statisticalLimitService.deleteById(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Limite estatístico excluído com sucesso!"));
    }
}
