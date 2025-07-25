package com.geosegbar.infra.reading.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.reading.dtos.BulkToggleActiveRequestDTO;
import com.geosegbar.infra.reading.dtos.BulkToggleActiveResponseDTO;
import com.geosegbar.infra.reading.dtos.InstrumentLimitStatusDTO;
import com.geosegbar.infra.reading.dtos.PagedReadingResponseDTO;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.dtos.ReadingResponseDTO;
import com.geosegbar.infra.reading.dtos.UpdateCommentRequestDTO;
import com.geosegbar.infra.reading.dtos.UpdateReadingRequestDTO;
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
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {

        PagedReadingResponseDTO<ReadingResponseDTO> readings = readingService.findByFilters(
                instrumentId, outputId, startDate, endDate, limitStatus, active, pageable);

        return ResponseEntity.ok(WebResponseEntity.success(readings, "Leituras obtidas com sucesso!"));
    }

    @GetMapping("/instrument/{instrumentId}/grouped")
    public ResponseEntity<WebResponseEntity<PagedReadingResponseDTO<ReadingResponseDTO>>> getGroupedReadingsByInstrument(
            @PathVariable Long instrumentId,
            Pageable pageable) {
        PagedReadingResponseDTO<ReadingResponseDTO> result = readingService.findGroupedReadingsFlatByInstrument(instrumentId, pageable);
        return ResponseEntity.ok(WebResponseEntity.success(result, "Leituras agrupadas obtidas com sucesso!"));
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

    @GetMapping("/instrument/{instrumentId}/limit-status")
    public ResponseEntity<WebResponseEntity<InstrumentLimitStatusDTO>> getInstrumentLimitStatus(
            @PathVariable Long instrumentId,
            @RequestParam(defaultValue = "10") int limit) {

        InstrumentLimitStatusDTO status = readingService.getInstrumentLimitStatus(instrumentId, limit);

        return ResponseEntity.ok(WebResponseEntity.success(
                status,
                "Status do limite do instrumento obtido com sucesso!"
        ));
    }

    @GetMapping("/client/{clientId}/instruments-limit-status")
    public ResponseEntity<WebResponseEntity<List<InstrumentLimitStatusDTO>>> getAllInstrumentLimitStatusesByClientId(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "10") int limit) {

        List<InstrumentLimitStatusDTO> statuses = readingService.getAllInstrumentLimitStatusesByClientId(clientId, limit);

        return ResponseEntity.ok(WebResponseEntity.success(
                statuses,
                "Status dos limites dos instrumentos do cliente obtidos com sucesso!"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ReadingResponseDTO>> updateReading(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReadingRequestDTO request) {

        ReadingResponseDTO updated = readingService.updateReading(id, request);

        return ResponseEntity.ok(WebResponseEntity.success(updated, "Leitura atualizada com sucesso!"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<WebResponseEntity<Void>> deactivateReading(@PathVariable Long id) {
        readingService.deactivate(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Leitura desativada com sucesso!"));
    }

    @PatchMapping("/bulk-toggle-active")
    public ResponseEntity<WebResponseEntity<BulkToggleActiveResponseDTO>> bulkToggleActive(
            @Valid @RequestBody BulkToggleActiveRequestDTO request) {

        BulkToggleActiveResponseDTO result = readingService.bulkToggleActive(
                request.getActive(),
                request.getReadingIds()
        );

        String action = request.getActive() ? "ativação" : "desativação";
        String message;

        if (result.getFailureCount() == 0) {
            message = String.format("Todas as %d leituras foram %s com sucesso!",
                    result.getSuccessCount(),
                    request.getActive() ? "ativadas" : "desativadas");
        } else if (result.getSuccessCount() == 0) {
            message = String.format("Falha na %s de todas as %d leituras!",
                    action,
                    result.getTotalProcessed());
        } else {
            message = String.format("Operação de %s concluída: %d sucessos, %d falhas de %d total",
                    action,
                    result.getSuccessCount(),
                    result.getFailureCount(),
                    result.getTotalProcessed());
        }

        return ResponseEntity.ok(WebResponseEntity.success(result, message));
    }

    @PatchMapping("/{id}/comment")
    public ResponseEntity<WebResponseEntity<ReadingResponseDTO>> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequestDTO request) {

        ReadingResponseDTO updated = readingService.updateComment(id, request.getComment());

        String message = request.getComment() != null
                ? "Comentário atualizado com sucesso!"
                : "Comentário removido com sucesso!";

        return ResponseEntity.ok(WebResponseEntity.success(updated, message));
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
