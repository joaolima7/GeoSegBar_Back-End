package com.geosegbar.infra.section_rendering_config.persistence.jpa;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.SectionRenderingConfigEntity;

@Repository
public interface SectionRenderingConfigRepository extends JpaRepository<SectionRenderingConfigEntity, Long> {

    @EntityGraph(attributePaths = {"customLevels", "selectedInstruments", "selectedReservoirs", "section"})
    Optional<SectionRenderingConfigEntity> findBySectionId(Long sectionId);

    @Modifying
    @Query(value = "DELETE FROM section_rendering_selected_reservoirs WHERE reservoir_id IN :reservoirIds", nativeQuery = true)
    void deleteSelectedReservoirsByReservoirIds(@Param("reservoirIds") Collection<Long> reservoirIds);
}
