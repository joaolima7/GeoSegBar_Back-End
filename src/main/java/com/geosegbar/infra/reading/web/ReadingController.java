package com.geosegbar.infra.reading.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.reading.dtos.PagedReadingResponseDTO;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.dtos.ReadingResponseDTO;
import com.geosegbar.infra.reading.services.ReadingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/readings")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;

    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<PagedReadingResponseDTO<ReadingResponseDTO>>> getReadingsByInstrument(
            @PathVariable Long instrumentId,
            @RequestParam(required = false) Long outputId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) LimitStatusEnum limitStatus,
            Pageable pageable) {

        PagedReadingResponseDTO<ReadingResponseDTO> readings = readingService.findByFilters(
                instrumentId, outputId, startDate, endDate, limitStatus, pageable);

        return ResponseEntity.ok(WebResponseEntity.success(readings, "Leituras obtidas com sucesso!"));
    }

    @GetMapping("/output/{outputId}")
    public ResponseEntity<WebResponseEntity<List<ReadingResponseDTO>>> getReadingsByOutput(
            @PathVariable Long outputId) {

        List<ReadingResponseDTO> readings = readingService.findByOutputId(outputId);

        return ResponseEntity.ok(WebResponseEntity.success(readings, "Leituras obtidas com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ReadingResponseDTO>> getReadingById(@PathVariable Long id) {
        ReadingResponseDTO reading = readingService.mapToResponseDTO(readingService.findById(id));
        return ResponseEntity.ok(WebResponseEntity.success(reading, "Leitura obtida com sucesso!"));
    }

    @PostMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<List<ReadingResponseDTO>>> createReading(
            @PathVariable Long instrumentId,
            @Valid @RequestBody ReadingRequestDTO request) {
        List<ReadingResponseDTO> created = readingService.create(instrumentId, request);
        return new ResponseEntity<>(WebResponseEntity.success(created, "Leituras registradas com sucesso!"), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteReading(@PathVariable Long id) {
        readingService.delete(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Leitura excluída com sucesso!"));
    }
}
