package com.geosegbar.infra.instrument_tabulate_pattern_folder.web;

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
import com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos.CreateTabulateFolderRequestDTO;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos.DamTabulateFoldersWithPatternsDetailResponseDTO;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos.TabulateFolderResponseDTO;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos.UpdateFolderNameDTO;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.services.InstrumentTabulatePatternFolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tabulate-pattern-folders")
@RequiredArgsConstructor
public class InstrumentTabulatePatternFolderController {

    private final InstrumentTabulatePatternFolderService folderService;

    @PostMapping
    public ResponseEntity<WebResponseEntity<TabulateFolderResponseDTO>> createFolder(
            @Valid @RequestBody CreateTabulateFolderRequestDTO request) {
        TabulateFolderResponseDTO dto = folderService.create(request);
        return new ResponseEntity<>(
                WebResponseEntity.success(dto, "Pasta de padrões de tabela criada com sucesso!"),
                HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteFolder(@PathVariable Long id) {
        folderService.delete(id);
        return ResponseEntity.ok(
                WebResponseEntity.success(null, "Pasta de padrões de tabela excluída com sucesso!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<TabulateFolderResponseDTO>> updateFolderName(
            @PathVariable Long id, @Valid @RequestBody UpdateFolderNameDTO request) {
        TabulateFolderResponseDTO dto = folderService.updateFolderName(id, request.getName());
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Pasta de padrões de tabela atualizada com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<TabulateFolderResponseDTO>> getFolderById(@PathVariable Long id) {
        TabulateFolderResponseDTO dto = folderService.findByIdSimple(id);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Pasta de padrões de tabela obtida com sucesso!"));
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<TabulateFolderResponseDTO>>> getFoldersByDam(@PathVariable Long damId) {
        List<TabulateFolderResponseDTO> dto = folderService.findByDamId(damId);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Pastas de padrões de tabela da barragem obtidas com sucesso!"));
    }

    @GetMapping("/dam/{damId}/patterns-detail")
    public ResponseEntity<WebResponseEntity<DamTabulateFoldersWithPatternsDetailResponseDTO>> getDamFoldersWithPatternsDetail(
            @PathVariable Long damId) {
        DamTabulateFoldersWithPatternsDetailResponseDTO dto = folderService.findFoldersWithPatternsDetailsByDam(damId);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Pastas de padrões de tabela com detalhes obtidas com sucesso!"));
    }
}
