package com.geosegbar.infra.instrument_tabulate_pattern.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.CreateTabulatePatternRequestDTO;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.TabulatePatternResponseDTO;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.UpdateTabulatePatternRequestDTO;
import com.geosegbar.infra.instrument_tabulate_pattern.services.InstrumentTabulatePatternService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tabulate-patterns")
@RequiredArgsConstructor
public class InstrumentTabulatePatternController {

    private final InstrumentTabulatePatternService patternService;

    @PostMapping
    public ResponseEntity<WebResponseEntity<TabulatePatternResponseDTO>> createPattern(
            @Valid @RequestBody CreateTabulatePatternRequestDTO request) {
        TabulatePatternResponseDTO dto = patternService.create(request);
        return new ResponseEntity<>(
                WebResponseEntity.success(dto, "Padrão de tabela criado com sucesso!"),
                HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deletePattern(@PathVariable Long id) {
        patternService.delete(id);
        return ResponseEntity.ok(
                WebResponseEntity.success(null, "Padrão de tabela excluído com sucesso!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<TabulatePatternResponseDTO>> updatePattern(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTabulatePatternRequestDTO request) {
        TabulatePatternResponseDTO dto = patternService.update(id, request);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Padrão de tabela atualizado com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<TabulatePatternResponseDTO>> getPatternById(@PathVariable Long id) {
        TabulatePatternResponseDTO dto = patternService.findById(id);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Padrão de tabela obtido com sucesso!"));
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<TabulatePatternResponseDTO>>> getPatternsByDam(@PathVariable Long damId) {
        List<TabulatePatternResponseDTO> dto = patternService.findByDamId(damId);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Padrões de tabela da barragem obtidos com sucesso!"));
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<WebResponseEntity<List<TabulatePatternResponseDTO>>> getPatternsByFolder(@PathVariable Long folderId) {
        List<TabulatePatternResponseDTO> dto = patternService.findByFolderId(folderId);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Padrões de tabela da pasta obtidos com sucesso!"));
    }
}
