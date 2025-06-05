package com.geosegbar.infra.hydrotelemetric.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.hydrotelemetric.jobs.HydrotelemetricDataCollectionJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/hydrotelemetric-data")
@RequiredArgsConstructor
@Slf4j
public class HydrotelemetricDataCollectionController {

    private final HydrotelemetricDataCollectionJob dataCollectionJob;

    @PostMapping("/collect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WebResponseEntity<Void>> collectDataManually() {
        log.info("Iniciando coleta manual de dados hidrotelemétricos");
        dataCollectionJob.collectDataManually();
        return ResponseEntity.ok(WebResponseEntity.success(null, "Coleta de dados iniciada com sucesso"));
    }
}
