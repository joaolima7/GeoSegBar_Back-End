package com.geosegbar.infra.section_rendering_config.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.section_rendering_config.dtos.SectionRenderDataDTO;
import com.geosegbar.infra.section_rendering_config.dtos.SectionRenderingConfigResponseDTO;
import com.geosegbar.infra.section_rendering_config.dtos.UpdateSectionRenderingConfigRequest;
import com.geosegbar.infra.section_rendering_config.services.SectionRenderingConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/section-rendering-config")
@RequiredArgsConstructor
public class SectionRenderingConfigController {

    private final SectionRenderingConfigService service;

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<WebResponseEntity<SectionRenderingConfigResponseDTO>> getBySection(
            @PathVariable Long sectionId) {
        SectionRenderingConfigResponseDTO dto = service.getBySectionId(sectionId);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Configuração de renderização obtida com sucesso!"));
    }

    @PutMapping("/section/{sectionId}")
    public ResponseEntity<WebResponseEntity<SectionRenderingConfigResponseDTO>> upsertBySection(
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateSectionRenderingConfigRequest request) {
        SectionRenderingConfigResponseDTO dto = service.upsert(sectionId, request);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Configuração de renderização salva com sucesso!"));
    }

    @GetMapping("/section/{sectionId}/render-data")
    public ResponseEntity<WebResponseEntity<SectionRenderDataDTO>> getRenderData(
            @PathVariable Long sectionId) {
        SectionRenderDataDTO dto = service.getRenderData(sectionId);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Dados de renderização da seção obtidos com sucesso!"));
    }
}
