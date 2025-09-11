package com.geosegbar.infra.reading.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.reading.dtos.ImportReadingsResult;
import com.geosegbar.infra.reading.services.BulkReadingImportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/readings-massive")
@RequiredArgsConstructor
public class ReadingImportController {

    private final BulkReadingImportService importService;

    @PostMapping
    public ResponseEntity<WebResponseEntity<ImportReadingsResult>> importExcel(
            @RequestParam(value = "instrumentId", required = true) Long instrumentId,
            @RequestPart(value = "file", required = true) MultipartFile file
    ) {

        if (instrumentId == null) {
            throw new InvalidInputException("ID do instrumento não fornecido. Por favor, informe o instrumento para importação das leituras.");
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("Nenhum arquivo foi enviado. Por favor, selecione uma planilha Excel válida.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            throw new InvalidInputException("Formato de arquivo inválido. Por favor, envie um arquivo Excel (.xlsx ou .xls).");
        }

        ImportReadingsResult result = importService.importFromExcel(instrumentId, file);

        String message = String.format(
                "Importação concluída: %d leituras processadas (%d com sucesso, %d falhas)",
                result.getTotalRows(),
                result.getSuccessCount(),
                result.getFailureCount()
        );

        return ResponseEntity.ok(WebResponseEntity.success(result, message));
    }
}
