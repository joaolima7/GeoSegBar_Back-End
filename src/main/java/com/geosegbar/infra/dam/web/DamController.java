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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.infra.dam.dtos.CreateDamCompleteRequest;
import com.geosegbar.infra.dam.dtos.UpdateDamRequest;
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

    @GetMapping("/filter")
    public ResponseEntity<WebResponseEntity<List<DamEntity>>> getDamsByClientAndStatus(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long statusId) {

        List<DamEntity> dams = damService.findByClientAndStatus(clientId, statusId);
        WebResponseEntity<List<DamEntity>> response = WebResponseEntity.success(
                dams,
                "Barragens filtradas obtidas com sucesso!"
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<WebResponseEntity<List<DamEntity>>> getDamsByClientId(@PathVariable Long clientId) {
        List<DamEntity> dams = damService.findDamsByClientId(clientId);
        WebResponseEntity<List<DamEntity>> response = WebResponseEntity.success(dams, "Barragens do cliente obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete")
    public ResponseEntity<WebResponseEntity<DamEntity>> createCompleteDam(@Valid @RequestBody CreateDamCompleteRequest request) {
        DamEntity createdDam = damService.createCompleteWithRelationships(request);
        WebResponseEntity<DamEntity> response = WebResponseEntity.success(createdDam, "Barragem completa criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<DamEntity>> createDam(@Valid @RequestBody DamEntity dam) {
        DamEntity createdDam = damService.save(dam);
        WebResponseEntity<DamEntity> response = WebResponseEntity.success(createdDam, "Barragem criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<DamEntity>> updateDam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDamRequest request) {

        DamEntity updatedDam = damService.updateBasicInfo(id, request);

        return ResponseEntity.ok(WebResponseEntity.success(
                updatedDam,
                "Barragem atualizada com sucesso!"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteDam(@PathVariable Long id) {
        damService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Barragem excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}
