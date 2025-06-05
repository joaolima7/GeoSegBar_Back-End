package com.geosegbar.infra.hydrotelemetric.web;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.HydrotelemetricReadingEntity;
import com.geosegbar.infra.hydrotelemetric.services.HydrotelemetricReadingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/hydrotelemetric/readings")
@RequiredArgsConstructor
@Slf4j
public class HydrotelemetricReadingController {

    private final HydrotelemetricReadingService hydrotelemetricReadingService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<Map<String, Object>>> getReadings(
            @RequestParam(required = false) Long damId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<HydrotelemetricReadingEntity> readingsPage = hydrotelemetricReadingService
                .getFilteredReadingsPaginated(damId, startDate, endDate, sortOrder, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("readings", readingsPage.getContent());
        response.put("currentPage", readingsPage.getNumber());
        response.put("totalItems", readingsPage.getTotalElements());
        response.put("totalPages", readingsPage.getTotalPages());
        response.put("hasNext", readingsPage.hasNext());
        response.put("hasPrevious", readingsPage.hasPrevious());

        return ResponseEntity.ok(WebResponseEntity.success(
                response, "Leituras hidrotelétricas obtidas com sucesso!"));
    }

    @GetMapping("/all")
    public ResponseEntity<WebResponseEntity<List<HydrotelemetricReadingEntity>>> getAllReadings(
            @RequestParam(required = false) Long damId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        List<HydrotelemetricReadingEntity> readings = hydrotelemetricReadingService
                .getFilteredReadings(damId, startDate, endDate, sortOrder);

        return ResponseEntity.ok(WebResponseEntity.success(
                readings, "Todas as leituras hidrotelétricas obtidas com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<HydrotelemetricReadingEntity>> getReadingById(@PathVariable Long id) {
        HydrotelemetricReadingEntity reading = hydrotelemetricReadingService.getReadingById(id);
        return ResponseEntity.ok(WebResponseEntity.success(
                reading, "Leitura hidrotelétrica obtida com sucesso!"));
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<HydrotelemetricReadingEntity>>> getReadingsByDamId(@PathVariable Long damId) {
        List<HydrotelemetricReadingEntity> readings = hydrotelemetricReadingService.getReadingsByDamId(damId);
        return ResponseEntity.ok(WebResponseEntity.success(
                readings, "Leituras hidrotelétricas para a barragem obtidas com sucesso!"));
    }

    @GetMapping("/dam/{damId}/latest")
    public ResponseEntity<WebResponseEntity<List<HydrotelemetricReadingEntity>>> getLatestReadingsByDamId(@PathVariable Long damId) {
        List<HydrotelemetricReadingEntity> readings = hydrotelemetricReadingService.getReadingsByDamIdOrderedByDateDesc(damId);
        return ResponseEntity.ok(WebResponseEntity.success(
                readings, "Leituras hidrotelétricas mais recentes para a barragem obtidas com sucesso!"));
    }

    @GetMapping("/date-range")
    public ResponseEntity<WebResponseEntity<List<HydrotelemetricReadingEntity>>> getReadingsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        List<HydrotelemetricReadingEntity> readings;
        if ("asc".equalsIgnoreCase(sortOrder)) {
            readings = hydrotelemetricReadingService.getReadingsByDateRange(startDate, endDate);
        } else {
            readings = hydrotelemetricReadingService.getReadingsByDateRange(startDate, endDate);
        }

        return ResponseEntity.ok(WebResponseEntity.success(
                readings, "Leituras hidrotelétricas por intervalo de datas obtidas com sucesso!"));
    }
}
