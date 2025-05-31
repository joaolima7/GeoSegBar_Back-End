package com.geosegbar.infra.section.web;

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
import com.geosegbar.entities.SectionEntity;
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

    @PostMapping
    public ResponseEntity<WebResponseEntity<SectionEntity>> createSection(@Valid @RequestBody SectionEntity section) {
        SectionEntity createdSection = sectionService.create(section);
        return new ResponseEntity<>(WebResponseEntity.success(createdSection, "Seção criada com sucesso!"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<SectionEntity>> updateSection(
            @PathVariable Long id,
            @Valid @RequestBody SectionEntity section) {
        SectionEntity updatedSection = sectionService.update(id, section);
        return ResponseEntity.ok(WebResponseEntity.success(updatedSection, "Seção atualizada com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteSection(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Seção excluída com sucesso!"));
    }
}
