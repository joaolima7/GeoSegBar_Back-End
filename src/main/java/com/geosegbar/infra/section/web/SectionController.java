package com.geosegbar.infra.section.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.SectionEntity;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.section.dtos.CreateSectionDTO;
import com.geosegbar.infra.section.dtos.SectionResponseDTO;
import com.geosegbar.infra.section.services.SectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;
    private final DamService damService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<SectionResponseDTO>>> getSectionsByFilters(
            @RequestParam(required = false) Long damId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(defaultValue = "false") boolean withDam) {
        List<SectionResponseDTO> sections = sectionService.getSectionsByFilters(damId, sectionId, withDam);
        return ResponseEntity.ok(WebResponseEntity.success(sections, "Seções obtidas com sucesso!"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebResponseEntity<SectionEntity>> createSection(
            @RequestPart("section") @Valid CreateSectionDTO sectionDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        SectionEntity createdSection;
        if (file != null && !file.isEmpty()) {
            createdSection = sectionService.createWithFile(sectionDTO, file);
        } else {
            SectionEntity section = new SectionEntity();
            section.setName(sectionDTO.getName());
            section.setFirstVertexLatitude(sectionDTO.getFirstVertexLatitude());
            section.setSecondVertexLatitude(sectionDTO.getSecondVertexLatitude());
            section.setFirstVertexLongitude(sectionDTO.getFirstVertexLongitude());
            section.setSecondVertexLongitude(sectionDTO.getSecondVertexLongitude());
            section.setDam(damService.findById(sectionDTO.getDamId()));
            createdSection = sectionService.create(section);
        }

        return new ResponseEntity<>(
                WebResponseEntity.success(createdSection, "Seção criada com sucesso!"),
                HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebResponseEntity<SectionEntity>> updateSection(
            @PathVariable Long id,
            @RequestPart("section") @Valid CreateSectionDTO sectionDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        SectionEntity updatedSection;
        if (file != null && !file.isEmpty()) {
            updatedSection = sectionService.updateWithFile(id, sectionDTO, file);
        } else {
            SectionEntity section = new SectionEntity();
            section.setName(sectionDTO.getName());
            section.setFirstVertexLatitude(sectionDTO.getFirstVertexLatitude());
            section.setSecondVertexLatitude(sectionDTO.getSecondVertexLatitude());
            section.setFirstVertexLongitude(sectionDTO.getFirstVertexLongitude());
            section.setSecondVertexLongitude(sectionDTO.getSecondVertexLongitude());
            section.setDam(damService.findById(sectionDTO.getDamId()));
            updatedSection = sectionService.update(id, section);
        }

        return ResponseEntity.ok(WebResponseEntity.success(updatedSection, "Seção atualizada com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteSection(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Seção excluída com sucesso!"));
    }
}
