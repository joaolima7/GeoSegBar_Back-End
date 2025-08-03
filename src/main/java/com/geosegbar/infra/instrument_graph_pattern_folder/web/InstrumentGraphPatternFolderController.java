package com.geosegbar.infra.instrument_graph_pattern_folder.web;

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
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.CreateFolderRequestDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.DamFoldersWithPatternsDetailResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.FolderResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.FolderWithPatternsDetailResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.UpdateFolderRequestDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.services.InstrumentGraphPatternFolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/graph-pattern-folders")
@RequiredArgsConstructor
public class InstrumentGraphPatternFolderController {

    private final InstrumentGraphPatternFolderService folderService;

    @PostMapping
    public ResponseEntity<WebResponseEntity<FolderResponseDTO>> createFolder(
            @Valid @RequestBody CreateFolderRequestDTO request) {
        FolderResponseDTO dto = folderService.create(request);
        return new ResponseEntity<>(
                WebResponseEntity.success(dto, "Pasta criada com sucesso!"),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<FolderResponseDTO>> updateFolder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFolderRequestDTO request) {
        FolderResponseDTO dto = folderService.update(id, request);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Pasta atualizada com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteFolder(@PathVariable Long id) {
        folderService.delete(id);
        return ResponseEntity.ok(
                WebResponseEntity.success(null, "Pasta excluída com sucesso!"));
    }

    @GetMapping("/dam/{damId}/patterns-detail")
    public ResponseEntity<WebResponseEntity<DamFoldersWithPatternsDetailResponseDTO>> getDamFoldersWithPatternsDetail(
            @PathVariable Long damId) {
        DamFoldersWithPatternsDetailResponseDTO dto = folderService.findFoldersWithPatternsDetailsByDam(damId);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Pastas da barragem com patterns detalhados obtidas com sucesso!"));
    }

    @GetMapping("/{id}/patterns-detail")
    public ResponseEntity<WebResponseEntity<FolderWithPatternsDetailResponseDTO>> getFolderWithPatternsDetail(
            @PathVariable Long id) {
        FolderWithPatternsDetailResponseDTO dto = folderService.findByIdWithPatternsDetails(id);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Pasta com patterns detalhados obtida com sucesso!"));
    }
}
