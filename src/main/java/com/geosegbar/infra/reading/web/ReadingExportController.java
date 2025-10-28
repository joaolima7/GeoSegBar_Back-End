package com.geosegbar.infra.reading.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.infra.reading.dtos.ReadingExportRequestDTO;
import com.geosegbar.infra.reading.services.ReadingExportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/readings/export")
@RequiredArgsConstructor
@Slf4j
public class ReadingExportController {

    private final ReadingExportService readingExportService;

    @PostMapping()
    public ResponseEntity<?> exportToExcel(
            @Valid @RequestBody ReadingExportRequestDTO request) {

        try {
            ByteArrayResource resource = readingExportService.exportToExcel(request);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "leituras_export_" + timestamp + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

            headers.add("X-Web-Success", "true");
            headers.add("X-Web-Message", "Exportação de leituras gerada com sucesso!");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception ex) {

            String msg = ex.getMessage() != null ? ex.getMessage() : "Erro ao gerar arquivo!";
            return ResponseEntity.status(500)
                    .body(com.geosegbar.common.response.WebResponseEntity.error(msg));
        }
    }
}
