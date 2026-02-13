package com.geosegbar.infra.documentation_dam.web;

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
import com.geosegbar.entities.DocumentationDamEntity;
import com.geosegbar.infra.documentation_dam.dtos.DocumentationDamDTO;
import com.geosegbar.infra.documentation_dam.dtos.DocumentationDamResponseDTO;
import com.geosegbar.infra.documentation_dam.services.DocumentationDamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/documentation-dam")
@RequiredArgsConstructor
public class DocumentationDamController {

    private final DocumentationDamService documentationDamService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<DocumentationDamEntity>>> getAllDocumentations() {
        List<DocumentationDamEntity> documentations = documentationDamService.findAll();
        WebResponseEntity<List<DocumentationDamEntity>> response
                = WebResponseEntity.success(documentations, "Documentações de barragens obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint performático que retorna todas as documentações sem carregar a
     * entidade Dam completa. Ideal para listagens e grids onde não é necessário
     * informações detalhadas da barragem.
     *
     * Performance: ~50-70% mais rápido que GET /documentation-dam
     *
     * @return Lista de DocumentationDamResponseDTO contendo apenas campos da
     * documentação + damId
     */
    @GetMapping("/lightweight")
    public ResponseEntity<WebResponseEntity<List<DocumentationDamResponseDTO>>> getAllDocumentationsLightweight() {
        List<DocumentationDamResponseDTO> documentations = documentationDamService.findAllLightweight();
        WebResponseEntity<List<DocumentationDamResponseDTO>> response
                = WebResponseEntity.success(documentations, "Documentações de barragens obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<DocumentationDamEntity>> getDocumentationById(@PathVariable Long id) {
        DocumentationDamEntity documentation = documentationDamService.findById(id);
        WebResponseEntity<DocumentationDamEntity> response
                = WebResponseEntity.success(documentation, "Documentação de barragem obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint performático que retorna uma documentação por ID sem carregar a
     * entidade Dam completa.
     *
     * @param id ID da documentação
     * @return DocumentationDamResponseDTO contendo apenas campos da
     * documentação + damId
     */
    @GetMapping("/{id}/lightweight")
    public ResponseEntity<WebResponseEntity<DocumentationDamResponseDTO>> getDocumentationByIdLightweight(@PathVariable Long id) {
        DocumentationDamResponseDTO documentation = documentationDamService.findByIdLightweight(id);
        WebResponseEntity<DocumentationDamResponseDTO> response
                = WebResponseEntity.success(documentation, "Documentação de barragem obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<DocumentationDamEntity>> getDocumentationByDamId(@PathVariable Long damId) {
        DocumentationDamEntity documentation = documentationDamService.findByDamId(damId);
        WebResponseEntity<DocumentationDamEntity> response
                = WebResponseEntity.success(documentation, "Documentação de barragem obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint performático que retorna documentação por damId sem carregar a
     * entidade Dam completa.
     *
     * @param damId ID da barragem
     * @return DocumentationDamResponseDTO contendo apenas campos da
     * documentação + damId
     */
    @GetMapping("/dam/{damId}/lightweight")
    public ResponseEntity<WebResponseEntity<DocumentationDamResponseDTO>> getDocumentationByDamIdLightweight(@PathVariable Long damId) {
        DocumentationDamResponseDTO documentation = documentationDamService.findByDamIdLightweight(damId);
        WebResponseEntity<DocumentationDamResponseDTO> response
                = WebResponseEntity.success(documentation, "Documentação de barragem obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<DocumentationDamEntity>> createDocumentation(
            @Valid @RequestBody DocumentationDamDTO documentationDTO) {
        DocumentationDamEntity createdDocumentation = documentationDamService.createOrUpdate(documentationDTO);
        WebResponseEntity<DocumentationDamEntity> response
                = WebResponseEntity.success(createdDocumentation, "Documentação de barragem criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<DocumentationDamEntity>> updateDocumentation(
            @PathVariable Long id,
            @Valid @RequestBody DocumentationDamDTO documentationDTO) {
        documentationDTO.setId(id);
        DocumentationDamEntity updatedDocumentation = documentationDamService.createOrUpdate(documentationDTO);
        WebResponseEntity<DocumentationDamEntity> response
                = WebResponseEntity.success(updatedDocumentation, "Documentação de barragem atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteDocumentation(@PathVariable Long id) {
        documentationDamService.delete(id);
        WebResponseEntity<Void> response
                = WebResponseEntity.success(null, "Documentação de barragem excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}
