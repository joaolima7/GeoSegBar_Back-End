package com.geosegbar.infra.instrument_graph_pattern.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.instrument_graph_pattern.dtos.CreateGraphPatternRequest;
import com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternDetailResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern.dtos.UpdateNameGraphPatternDTO;
import com.geosegbar.infra.instrument_graph_pattern.services.InstrumentGraphPatternService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/graph-patterns")
@RequiredArgsConstructor
public class InstrumentGraphPatternController {

    private final InstrumentGraphPatternService patternService;

    @PostMapping
    public ResponseEntity<WebResponseEntity<GraphPatternResponseDTO>> createPattern(
            @Valid @RequestBody CreateGraphPatternRequest request) {
        GraphPatternResponseDTO dto = patternService.create(request);
        return new ResponseEntity<>(
                WebResponseEntity.success(dto, "Padrão de Gráfico criado com sucesso!"),
                HttpStatus.CREATED);
    }

    @PatchMapping("/name/{id}")
    public ResponseEntity<WebResponseEntity<GraphPatternDetailResponseDTO>> updateNameGraphPattern(
            @Valid @RequestBody UpdateNameGraphPatternDTO request, @PathVariable Long id) {
        GraphPatternDetailResponseDTO dto = patternService.updateNameGraphPattern(id, request.getName());
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Nome do Padrão de Gráfico atualizado com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deletePattern(@PathVariable Long id) {
        patternService.deleteById(id);
        return ResponseEntity.ok(
                WebResponseEntity.success(null, "Padrão de Gráfico excluído com sucesso!"));
    }

    @GetMapping("/simple/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<List<GraphPatternResponseDTO>>> getSimpleByInstrument(
            @PathVariable Long instrumentId) {
        List<GraphPatternResponseDTO> list = patternService.findByInstrument(instrumentId);
        return ResponseEntity.ok(
                WebResponseEntity.success(list, "Padrões de Gráfico obtidos com sucesso!"));
    }

    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<List<GraphPatternDetailResponseDTO>>> getByInstrument(
            @PathVariable Long instrumentId) {
        List<GraphPatternDetailResponseDTO> list = patternService.findByInstrumentWithDetails(instrumentId);
        return ResponseEntity.ok(
                WebResponseEntity.success(list, "Padrões de Gráfico completos obtidos com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<GraphPatternDetailResponseDTO>> getById(
            @PathVariable Long id) {
        GraphPatternDetailResponseDTO dto = patternService.findByIdWithDetails(id);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Padrão de Gráfico completo obtido com sucesso!"));
    }

    @GetMapping("/simple/{id}")
    public ResponseEntity<WebResponseEntity<GraphPatternResponseDTO>> getSimpleById(
            @PathVariable Long id) {
        GraphPatternResponseDTO dto = patternService.mapToResponseDTO(
                patternService.findById(id));
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Padrão de Gráfico obtido com sucesso!"));
    }
}
