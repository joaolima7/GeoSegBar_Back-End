package com.geosegbar.infra.reading.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.reading.dtos.ImportReadingsResult;
import com.geosegbar.infra.reading.services.BulkReadingImportService;

import lombok.RequiredArgsConstructor;

// com.geosegbar.infra.reading.web.ReadingImportController
@RestController
@RequestMapping("/readings-massive")
@RequiredArgsConstructor
public class ReadingImportController {

    private final BulkReadingImportService importService;

    @PostMapping
    public ResponseEntity<WebResponseEntity<ImportReadingsResult>> importExcel(
            @RequestParam("instrumentId") Long instrumentId,
            @RequestPart("file") MultipartFile file
    ) {
        ImportReadingsResult result = importService.importFromExcel(instrumentId, file);
        return ResponseEntity.ok(
                WebResponseEntity.success(result, "Importação de leituras concluída!")
        );
    }
}
