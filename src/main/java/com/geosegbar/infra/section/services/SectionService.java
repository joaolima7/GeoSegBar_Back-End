package com.geosegbar.infra.section.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.SectionEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.section.persistence.jpa.SectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionService {

    private final SectionRepository sectionRepository;

    public List<SectionEntity> findAll() {
        return sectionRepository.findAllByOrderByNameAsc();
    }

    public SectionEntity findById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seção não encontrada com ID: " + id));
    }

    public Optional<SectionEntity> findByName(String name) {
        return sectionRepository.findByName(name);
    }

    @Transactional
    public SectionEntity create(SectionEntity section) {
        if (sectionRepository.findByName(section.getName()).isPresent()) {
            throw new DuplicateResourceException("Seção com nome '" + section.getName() + "' já existe");
        }

        SectionEntity savedSection = sectionRepository.save(section);
        log.info("Nova seção criada: {}", savedSection.getName());
        return savedSection;
    }

    @Transactional
    public SectionEntity update(Long id, SectionEntity section) {
        SectionEntity existingSection = findById(id);

        if (sectionRepository.findByName(section.getName()).isPresent()
                && !existingSection.getName().equals(section.getName())) {
            throw new DuplicateResourceException("Seção com nome '" + section.getName() + "' já existe");
        }

        existingSection.setName(section.getName());
        existingSection.setFilePath(section.getFilePath());
        existingSection.setFirstVertexLatitude(section.getFirstVertexLatitude());
        existingSection.setSecondVertexLatitude(section.getSecondVertexLatitude());
        existingSection.setFirstVertexLongitude(section.getFirstVertexLongitude());
        existingSection.setSecondVertexLongitude(section.getSecondVertexLongitude());

        SectionEntity updatedSection = sectionRepository.save(existingSection);
        log.info("Seção atualizada: {}", updatedSection.getName());
        return updatedSection;
    }

    @Transactional
    public void delete(Long id) {
        SectionEntity section = findById(id);

        if (!section.getInstruments().isEmpty()) {
            throw new IllegalStateException("Não é possível excluir uma seção que possui instrumentos associados");
        }

        sectionRepository.delete(section);
        log.info("Seção excluída: {}", section.getName());
    }
}
