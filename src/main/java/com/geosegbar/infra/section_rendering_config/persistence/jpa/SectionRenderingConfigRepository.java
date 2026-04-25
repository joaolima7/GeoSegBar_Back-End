package com.geosegbar.infra.section_rendering_config.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.SectionRenderingConfigEntity;

@Repository
public interface SectionRenderingConfigRepository extends JpaRepository<SectionRenderingConfigEntity, Long> {

    @EntityGraph(attributePaths = {"customLevels", "selectedInstruments", "section"})
    Optional<SectionRenderingConfigEntity> findBySectionId(Long sectionId);
}
