package com.geosegbar.infra.map_kml.web;

import java.util.List;

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
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.map_kml.dtos.MapKmlFileResponseDTO;
import com.geosegbar.infra.map_kml.dtos.MapKmlFolderCreateDTO;
import com.geosegbar.infra.map_kml.dtos.MapKmlFolderResponseDTO;
import com.geosegbar.infra.map_kml.dtos.MapKmlFolderUpdateDTO;
import com.geosegbar.infra.map_kml.dtos.RenameKmlFileRequest;
import com.geosegbar.infra.map_kml.services.MapKmlFolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/map-kml")
@RequiredArgsConstructor
public class MapKmlController {

    private final MapKmlFolderService mapKmlFolderService;

    @GetMapping("/folders/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<MapKmlFolderResponseDTO>>> getFoldersByDam(@PathVariable Long damId) {
        List<MapKmlFolderResponseDTO> folders = mapKmlFolderService.findByDamId(damId);
        return ResponseEntity.ok(WebResponseEntity.success(folders, "Pastas KML obtidas com sucesso!"));
    }

    @PostMapping("/folders")
    public ResponseEntity<WebResponseEntity<MapKmlFolderResponseDTO>> createFolder(
            @Valid @RequestBody MapKmlFolderCreateDTO dto) {
        MapKmlFolderResponseDTO folder = mapKmlFolderService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WebResponseEntity.success(folder, "Pasta KML criada com sucesso!"));
    }

    @PutMapping("/folders/{id}")
    public ResponseEntity<WebResponseEntity<MapKmlFolderResponseDTO>> updateFolder(
            @PathVariable Long id,
            @Valid @RequestBody MapKmlFolderUpdateDTO dto) {
        MapKmlFolderResponseDTO folder = mapKmlFolderService.update(id, dto);
        return ResponseEntity.ok(WebResponseEntity.success(folder, "Pasta KML atualizada com sucesso!"));
    }

    @DeleteMapping("/folders/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteFolder(@PathVariable Long id) {
        mapKmlFolderService.delete(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Pasta KML excluída com sucesso!"));
    }

    @PostMapping("/files/upload/{folderId}")
    public ResponseEntity<WebResponseEntity<MapKmlFolderResponseDTO>> uploadFile(
            @PathVariable Long folderId,
            @RequestParam("file") MultipartFile file) {
        MapKmlFolderResponseDTO folder = mapKmlFolderService.uploadFile(folderId, file);
        return ResponseEntity.ok(WebResponseEntity.success(folder, "Arquivo KML enviado com sucesso!"));
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<WebResponseEntity<Void>> deleteFile(@PathVariable Long fileId) {
        mapKmlFolderService.deleteFile(fileId);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Arquivo KML excluído com sucesso!"));
    }

    @PutMapping("/files/{fileId}/overwrite")
    public ResponseEntity<WebResponseEntity<MapKmlFileResponseDTO>> overwriteFile(
            @PathVariable Long fileId,
            @RequestParam("file") MultipartFile file) {
        MapKmlFileResponseDTO dto = mapKmlFolderService.overwriteFile(fileId, file);
        return ResponseEntity.ok(WebResponseEntity.success(dto, "Arquivo KML sobrescrito com sucesso!"));
    }

    @PatchMapping("/files/{fileId}/rename")
    public ResponseEntity<WebResponseEntity<MapKmlFileResponseDTO>> renameFile(
            @PathVariable Long fileId,
            @Valid @RequestBody RenameKmlFileRequest request) {
        MapKmlFileResponseDTO dto = mapKmlFolderService.renameFile(fileId, request.getFilename());
        return ResponseEntity.ok(WebResponseEntity.success(dto, "Arquivo KML renomeado com sucesso!"));
    }
}
