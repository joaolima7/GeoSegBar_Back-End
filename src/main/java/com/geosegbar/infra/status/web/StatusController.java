package com.geosegbar.infra.status.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.infra.status.services.StatusService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/status")
@RequiredArgsConstructor
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<StatusEntity>>> getAllStatuses() {
        List<StatusEntity> statuses = statusService.findAll();
        WebResponseEntity<List<StatusEntity>> response = WebResponseEntity.success(
                statuses, "Status retrieved successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<StatusEntity>> getStatusById(@PathVariable Long id) {
        StatusEntity status = statusService.findById(id);
        WebResponseEntity<StatusEntity> response = WebResponseEntity.success(
                status, "Status retrieved successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<WebResponseEntity<StatusEntity>> getActiveStatus() {
        StatusEntity status = statusService.getActiveStatus();
        WebResponseEntity<StatusEntity> response = WebResponseEntity.success(
                status, "Active status retrieved successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/disabled")
    public ResponseEntity<WebResponseEntity<StatusEntity>> getDisabledStatus() {
        StatusEntity status = statusService.getDisabledStatus();
        WebResponseEntity<StatusEntity> response = WebResponseEntity.success(
                status, "Disabled status retrieved successfully!");
        return ResponseEntity.ok(response);
    }
}
