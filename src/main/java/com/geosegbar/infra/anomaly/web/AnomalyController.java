package com.geosegbar.infra.anomaly.web;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import com.geosegbar.infra.anomaly.dtos.AnomalyListItemDTO;
import com.geosegbar.infra.anomaly.dtos.PagedAnomalyDTO;
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
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.infra.anomaly.dtos.AnomalyDTO;
import com.geosegbar.infra.anomaly.dtos.UpdateAnomalyRequestDTO;
import com.geosegbar.infra.anomaly.services.AnomalyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyService anomalyService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<AnomalyEntity>>> getAllAnomalies() {
        List<AnomalyEntity> anomalies = anomalyService.findAll();
        WebResponseEntity<List<AnomalyEntity>> response = WebResponseEntity.success(
                anomalies, "Anomalias recuperadas com sucesso!");
        return ResponseEntity.ok(response);
    }

    /**
     * Listagem filtrada e paginada. Rota nova: a GET /anomalies existente
     * devolve todas e não deve ganhar parâmetro.
     */
    @GetMapping("/filter")
    public ResponseEntity<WebResponseEntity<PagedAnomalyDTO<AnomalyListItemDTO>>> getAnomaliesFiltered(
            @RequestParam(required = false) List<Long> damIds,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedAnomalyDTO<AnomalyListItemDTO> resultado = anomalyService.findByFilters(
                damIds, statusId, startDate, endDate, page, size);

        return ResponseEntity.ok(WebResponseEntity.success(
                resultado, "Anomalias obtidas com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<AnomalyEntity>> getAnomalyById(@PathVariable Long id) {
        AnomalyEntity anomaly = anomalyService.findById(id);
        WebResponseEntity<AnomalyEntity> response = WebResponseEntity.success(
                anomaly, "Anomalia recuperada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<AnomalyEntity>> updateAnomaly(
            @PathVariable Long id, @Valid @RequestBody UpdateAnomalyRequestDTO request) {
        AnomalyEntity anomaly = anomalyService.update(id, request);
        WebResponseEntity<AnomalyEntity> response = WebResponseEntity.success(
                anomaly, "Anomalia atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<AnomalyEntity>>> getAnomaliesByDamId(@PathVariable Long damId) {
        List<AnomalyEntity> anomalies = anomalyService.findByDamId(damId);
        WebResponseEntity<List<AnomalyEntity>> response = WebResponseEntity.success(
                anomalies, "Anomalias recuperadas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<AnomalyEntity>> createAnomaly(@Valid @RequestBody AnomalyDTO request) {
        AnomalyEntity anomaly = anomalyService.create(request);
        WebResponseEntity<AnomalyEntity> response = WebResponseEntity.success(
                anomaly, "Anomalia criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteAnomaly(@PathVariable Long id) {
        anomalyService.delete(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(
                null, "Anomalia excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}
