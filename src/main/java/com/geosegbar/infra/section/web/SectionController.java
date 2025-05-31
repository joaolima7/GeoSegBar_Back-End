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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.SectionEntity;
import com.geosegbar.infra.section.dtos.CreateSectionDTO;
import com.geosegbar.infra.section.services.SectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<SectionEntity>>> getAllSections() {
        List<SectionEntity> sections = sectionService.findAll();
        return ResponseEntity.ok(WebResponseEntity.success(sections, "Seções obtidas com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<SectionEntity>> getSectionById(@PathVariable Long id) {
        SectionEntity section = sectionService.findById(id);
        return ResponseEntity.ok(WebResponseEntity.success(section, "Seção obtida com sucesso!"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponseEntity<SectionEntity>> createSectionJson(
            @Valid @RequestBody CreateSectionDTO sectionDTO) {

        SectionEntity section = new SectionEntity();
        section.setName(sectionDTO.getName());
        section.setFirstVertexLatitude(sectionDTO.getFirstVertexLatitude());
        section.setSecondVertexLatitude(sectionDTO.getSecondVertexLatitude());
        section.setFirstVertexLongitude(sectionDTO.getFirstVertexLongitude());
        section.setSecondVertexLongitude(sectionDTO.getSecondVertexLongitude());

        SectionEntity createdSection = sectionService.create(section);

        return new ResponseEntity<>(
                WebResponseEntity.success(createdSection, "Seção criada com sucesso!"),
                HttpStatus.CREATED);
    }

    @PostMapping(path = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebResponseEntity<SectionEntity>> createSectionWithFile(
            @RequestPart("section") @Valid CreateSectionDTO sectionDTO,
            @RequestPart("file") MultipartFile file) {

        SectionEntity createdSection = sectionService.createWithFile(sectionDTO, file);

        return new ResponseEntity<>(
                WebResponseEntity.success(createdSection, "Seção criada com sucesso!"),
                HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponseEntity<SectionEntity>> updateSection(
            @PathVariable Long id,
            @Valid @RequestBody SectionEntity section) {
        SectionEntity updatedSection = sectionService.update(id, section);
        return ResponseEntity.ok(WebResponseEntity.success(updatedSection, "Seção atualizada com sucesso!"));
    }

    @PutMapping(value = "/{id}/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebResponseEntity<SectionEntity>> updateSectionWithFile(
            @PathVariable Long id,
            @RequestPart("section") @Valid CreateSectionDTO sectionDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        SectionEntity updatedSection = sectionService.updateWithFile(id, sectionDTO, file);
        return ResponseEntity.ok(WebResponseEntity.success(updatedSection, "Seção atualizada com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteSection(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Seção excluída com sucesso!"));
    }
}
