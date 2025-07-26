package com.geosegbar.infra.instrument.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.instrument.dtos.ImportInstrumentsRequest;
import com.geosegbar.infra.instrument.services.BulkInstrumentImportService;
import com.geosegbar.infra.instrument.services.BulkInstrumentImportService.ImportResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/instruments-massive")
@RequiredArgsConstructor
public class InstrumentImportController {

    private final BulkInstrumentImportService importService;

    @PostMapping
    public ResponseEntity<WebResponseEntity<ImportResult>> importExcel(
            @Valid @RequestPart("metadata") ImportInstrumentsRequest metadata,
            @RequestPart("file") MultipartFile file
    ) {
        ImportResult result = importService.importFromExcel(metadata, file);
        return ResponseEntity.ok(WebResponseEntity.success(result, "Importação concluída"));
    }

}
