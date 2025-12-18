package com.geosegbar.infra.share_folder.web;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.ShareFolderEntity;
import com.geosegbar.infra.share_folder.dtos.CreateShareFolderRequest;
import com.geosegbar.infra.share_folder.services.ShareFolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareFolderController {

    private final ShareFolderService shareFolderService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<WebResponseEntity<List<ShareFolderEntity>>> getSharesByUser(@PathVariable Long userId) {
        List<ShareFolderEntity> shares = shareFolderService.findAllByUser(userId);
        WebResponseEntity<List<ShareFolderEntity>> response = WebResponseEntity.success(
                shares, "Compartilhamentos obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<WebResponseEntity<List<ShareFolderEntity>>> getSharesByFolder(@PathVariable Long folderId) {
        List<ShareFolderEntity> shares = shareFolderService.findAllByFolder(folderId);
        WebResponseEntity<List<ShareFolderEntity>> response = WebResponseEntity.success(
                shares, "Compartilhamentos da pasta obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<ShareFolderEntity>> createShare(
            @Valid @RequestBody CreateShareFolderRequest request) {
        ShareFolderEntity share = shareFolderService.create(request);
        WebResponseEntity<ShareFolderEntity> response = WebResponseEntity.success(
                share, "Compartilhamento criado com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{shareId}")
    public ResponseEntity<WebResponseEntity<Void>> deactivateShare(@PathVariable Long shareId) {
        shareFolderService.deleteShare(shareId);
        WebResponseEntity<Void> response = WebResponseEntity.success(
                null, "Compartilhamento excluído com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/access/{token}")
    public ResponseEntity<WebResponseEntity<PSBFolderEntity>> accessSharedFolder(@PathVariable String token) {
        PSBFolderEntity folder = shareFolderService.registerAccessAndGetFolder(token);
        WebResponseEntity<PSBFolderEntity> response = WebResponseEntity.success(
                folder, "Pasta compartilhada obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<ShareFolderEntity>>> getSharesByDam(@PathVariable Long damId) {
        List<ShareFolderEntity> shares = shareFolderService.findAllByDamId(damId);
        WebResponseEntity<List<ShareFolderEntity>> response = WebResponseEntity.success(
                shares, "Compartilhamentos da barragem obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> downloadAllFiles(@PathVariable String token) {
        java.io.ByteArrayOutputStream zipStream = shareFolderService.downloadAllFiles(token);

        org.springframework.core.io.ByteArrayResource resource
                = new org.springframework.core.io.ByteArrayResource(zipStream.toByteArray());

        ShareFolderEntity share = shareFolderService.findByToken(token);
        String filename = sanitizeFilename(share.getPsbFolder().getName()) + "_arquivos.zip";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentLength(resource.contentLength())
                .body(resource);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
