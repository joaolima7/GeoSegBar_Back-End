package com.geosegbar.infra.deterministic_limit.web;

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
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.infra.deterministic_limit.services.DeterministicLimitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/deterministic-limits")
@RequiredArgsConstructor
public class DeterministicLimitController {

    private final DeterministicLimitService deterministicLimitService;

    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<DeterministicLimitEntity>> getByInstrumentId(@PathVariable Long instrumentId) {
        Optional<DeterministicLimitEntity> limit = deterministicLimitService.findByInstrumentId(instrumentId);
        return limit.map(l -> ResponseEntity.ok(WebResponseEntity.success(l, "Limite determinístico obtido com sucesso!")))
                .orElseGet(() -> ResponseEntity.ok(WebResponseEntity.success(null, "Instrumento não possui limite determinístico")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<DeterministicLimitEntity>> getById(@PathVariable Long id) {
        DeterministicLimitEntity limit = deterministicLimitService.findById(id);
        return ResponseEntity.ok(WebResponseEntity.success(limit, "Limite determinístico obtido com sucesso!"));
    }

    @PostMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<DeterministicLimitEntity>> create(
            @PathVariable Long instrumentId,
            @Valid @RequestBody DeterministicLimitEntity limit) {
        DeterministicLimitEntity created = deterministicLimitService.createOrUpdate(instrumentId, limit);
        return new ResponseEntity<>(WebResponseEntity.success(created, "Limite determinístico criado com sucesso!"), HttpStatus.CREATED);
    }

    @PutMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<DeterministicLimitEntity>> update(
            @PathVariable Long instrumentId,
            @Valid @RequestBody DeterministicLimitEntity limit) {
        DeterministicLimitEntity updated = deterministicLimitService.createOrUpdate(instrumentId, limit);
        return ResponseEntity.ok(WebResponseEntity.success(updated, "Limite determinístico atualizado com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> delete(@PathVariable Long id) {
        deterministicLimitService.deleteById(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Limite determinístico excluído com sucesso!"));
    }
}
