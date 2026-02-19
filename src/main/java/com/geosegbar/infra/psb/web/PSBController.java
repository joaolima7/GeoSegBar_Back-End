package com.geosegbar.infra.psb.web;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.infra.psb.dtos.CreatePSBFolderRequest;
import com.geosegbar.infra.psb.dtos.PresignedUploadCompleteRequest;
import com.geosegbar.infra.psb.dtos.PresignedUploadInitRequest;
import com.geosegbar.infra.psb.dtos.PresignedUploadInitResponse;
import com.geosegbar.infra.psb.services.PSBFileService;
import com.geosegbar.infra.psb.services.PSBFolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/psb")
@RequiredArgsConstructor
public class PSBController {

    private final PSBFolderService psbFolderService;
    private final PSBFileService psbFileService;

    @GetMapping("/folders/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<PSBFolderEntity>>> getFoldersByDamId(@PathVariable Long damId) {
        List<PSBFolderEntity> folders = psbFolderService.findAllByDamId(damId);
        WebResponseEntity<List<PSBFolderEntity>> response = WebResponseEntity.success(
                folders, "Pastas raiz PSB obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/folders/{parentFolderId}/subfolders")
    public ResponseEntity<WebResponseEntity<List<PSBFolderEntity>>> getSubfolders(@PathVariable Long parentFolderId) {
        List<PSBFolderEntity> subfolders = psbFolderService.findSubfolders(parentFolderId);
        WebResponseEntity<List<PSBFolderEntity>> response = WebResponseEntity.success(
                subfolders, "Subpastas obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/folders/dam/{damId}/complete")
    public ResponseEntity<WebResponseEntity<List<PSBFolderEntity>>> getCompleteHierarchy(@PathVariable Long damId) {
        List<PSBFolderEntity> completeStructure = psbFolderService.findCompleteHierarchyByDamId(damId);
        WebResponseEntity<List<PSBFolderEntity>> response = WebResponseEntity.success(
                completeStructure, "Estrutura completa PSB obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/folders/{id}")
    public ResponseEntity<WebResponseEntity<PSBFolderEntity>> getFolderById(@PathVariable Long id) {
        PSBFolderEntity folder = psbFolderService.findById(id);
        WebResponseEntity<PSBFolderEntity> response = WebResponseEntity.success(
                folder, "Pasta PSB obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/folders")
    public ResponseEntity<WebResponseEntity<PSBFolderEntity>> createFolder(
            @Valid @RequestBody CreatePSBFolderRequest request) {
        PSBFolderEntity folder = psbFolderService.create(request);
        WebResponseEntity<PSBFolderEntity> response = WebResponseEntity.success(
                folder, "Pasta PSB criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/folders/{id}")
    public ResponseEntity<WebResponseEntity<PSBFolderEntity>> updateFolder(
            @PathVariable Long id, @Valid @RequestBody CreatePSBFolderRequest request) {
        PSBFolderEntity folder = psbFolderService.update(id, request);
        WebResponseEntity<PSBFolderEntity> response = WebResponseEntity.success(
                folder, "Pasta PSB atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/folders/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteFolder(@PathVariable Long id) {
        psbFolderService.delete(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(
                null, "Pasta PSB excluída com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/files/folder/{folderId}")
    public ResponseEntity<WebResponseEntity<List<PSBFileEntity>>> getFilesByFolderId(@PathVariable Long folderId) {
        List<PSBFileEntity> files = psbFileService.findByFolderId(folderId);
        WebResponseEntity<List<PSBFileEntity>> response = WebResponseEntity.success(
                files, "Arquivos PSB obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/files/upload/{folderId}")
    public ResponseEntity<WebResponseEntity<PSBFileEntity>> uploadFile(
            @PathVariable Long folderId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedById") Long uploadedById) {
        PSBFileEntity psbFile = psbFileService.uploadFile(folderId, file, uploadedById);
        WebResponseEntity<PSBFileEntity> response = WebResponseEntity.success(
                psbFile, "Arquivo PSB enviado com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        PSBFileEntity file = psbFileService.findById(fileId);
        Resource resource = psbFileService.downloadFile(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<WebResponseEntity<Void>> deleteFile(@PathVariable Long fileId) {
        psbFileService.deleteFile(fileId);
        WebResponseEntity<Void> response = WebResponseEntity.success(
                null, "Arquivo PSB excluído com sucesso!");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PRESIGNED URL UPLOAD — Upload direto ao S3 sem passar pelo Spring
    // =========================================================================
    /**
     * FASE 1: Inicializa upload pré-assinado multipart. Retorna URLs para o
     * cliente enviar cada parte diretamente ao S3. TODOS os uploads usam
     * multipart — sem exceção.
     */
    @PostMapping("/files/presigned/init/{folderId}")
    public ResponseEntity<WebResponseEntity<PresignedUploadInitResponse>> initPresignedUpload(
            @PathVariable Long folderId,
            @Valid @RequestBody PresignedUploadInitRequest request) {
        PresignedUploadInitResponse response = psbFileService.initPresignedUpload(folderId, request);
        WebResponseEntity<PresignedUploadInitResponse> webResponse = WebResponseEntity.success(
                response, "Upload pré-assinado inicializado com sucesso!");
        return new ResponseEntity<>(webResponse, HttpStatus.CREATED);
    }

    /**
     * FASE 2: Confirma que o upload ao S3 foi concluído. Verifica o arquivo no
     * S3 e salva o registro no banco.
     */
    @PostMapping("/files/presigned/complete/{folderId}")
    public ResponseEntity<WebResponseEntity<PSBFileEntity>> completePresignedUpload(
            @PathVariable Long folderId,
            @Valid @RequestBody PresignedUploadCompleteRequest request) {
        PSBFileEntity psbFile = psbFileService.completePresignedUpload(folderId, request);
        WebResponseEntity<PSBFileEntity> webResponse = WebResponseEntity.success(
                psbFile, "Upload pré-assinado confirmado com sucesso!");
        return new ResponseEntity<>(webResponse, HttpStatus.CREATED);
    }

    /**
     * Cancela um upload pré-assinado em andamento.
     */
    @PostMapping("/files/presigned/abort/{uploadId}")
    public ResponseEntity<WebResponseEntity<Void>> abortPresignedUpload(@PathVariable String uploadId) {
        psbFileService.abortPresignedUpload(uploadId);
        WebResponseEntity<Void> webResponse = WebResponseEntity.success(
                null, "Upload pré-assinado cancelado com sucesso!");
        return ResponseEntity.ok(webResponse);
    }
}
