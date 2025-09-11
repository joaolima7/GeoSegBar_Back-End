package com.geosegbar.infra.instrument.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.exceptions.InvalidInputException;
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
            @Valid @RequestPart(value = "metadata", required = true) ImportInstrumentsRequest metadata,
            @RequestPart(value = "file", required = true) MultipartFile file
    ) {

        if (file.isEmpty()) {
            throw new InvalidInputException("O arquivo enviado está vazio. Por favor, selecione uma planilha Excel válida.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            throw new InvalidInputException("Formato de arquivo inválido. Por favor, envie um arquivo Excel (.xlsx ou .xls).");
        }

        ImportResult result = importService.importFromExcel(metadata, file);
        return ResponseEntity.ok(WebResponseEntity.success(result, "Importação concluída"));
    }

}
