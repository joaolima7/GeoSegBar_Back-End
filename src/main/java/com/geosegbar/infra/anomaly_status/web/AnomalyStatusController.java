package com.geosegbar.infra.anomaly_status.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.infra.anomaly_status.services.AnomalyStatusService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/anomaly-status")
@RequiredArgsConstructor
public class AnomalyStatusController {

    private final AnomalyStatusService anomalyStatusService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<AnomalyStatusEntity>>> getAllAnomalyStatuses() {
        List<AnomalyStatusEntity> anomalyStatuses = anomalyStatusService.findAll();
        WebResponseEntity<List<AnomalyStatusEntity>> response = WebResponseEntity.success(
                anomalyStatuses, "Anomaly statuses retrieved successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<AnomalyStatusEntity>> getAnomalyStatusById(@PathVariable Long id) {
        AnomalyStatusEntity anomalyStatus = anomalyStatusService.findById(id);
        WebResponseEntity<AnomalyStatusEntity> response = WebResponseEntity.success(
                anomalyStatus, "Anomaly status retrieved successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<WebResponseEntity<AnomalyStatusEntity>> getAnomalyStatusByName(@PathVariable String name) {
        AnomalyStatusEntity anomalyStatus = anomalyStatusService.findByName(name);
        WebResponseEntity<AnomalyStatusEntity> response = WebResponseEntity.success(
                anomalyStatus, "Anomaly status retrieved successfully!");
        return ResponseEntity.ok(response);
    }
}
