package com.geosegbar.infra.instrument.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.entities.SectionEntity;

@Repository
public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {

    List<InstrumentEntity> findAllByOrderByNameAsc();

    List<InstrumentEntity> findByDamId(Long damId);

    List<InstrumentEntity> findByDam(DamEntity dam);

    List<InstrumentEntity> findByInstrumentTypeId(Long instrumentTypeId);

    List<InstrumentEntity> findByInstrumentType(InstrumentTypeEntity instrumentType);

    List<InstrumentEntity> findBySectionId(Long sectionId);

    List<InstrumentEntity> findBySection(SectionEntity section);

    Optional<InstrumentEntity> findByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamIdAndIdNot(String name, Long damId, Long id);

    @EntityGraph(attributePaths = {"statisticalLimit", "deterministicLimit"})
    Optional<InstrumentEntity> findWithLimitsById(Long id);

    @EntityGraph(attributePaths = {"inputs", "constants", "outputs"})
    Optional<InstrumentEntity> findWithIOCById(Long id);

    @EntityGraph(attributePaths = {"statisticalLimit", "deterministicLimit", "inputs", "constants", "outputs"})
    Optional<InstrumentEntity> findWithAllDetailsById(Long id);

    @Query("SELECT i FROM InstrumentEntity i "
            + "WHERE (:damId IS NULL OR i.dam.id = :damId) "
            + "AND (:typeId IS NULL OR i.instrumentType.id = :typeId) "
            + "AND (:sectionId IS NULL OR i.section.id = :sectionId)")
    List<InstrumentEntity> findByFilters(
            @Param("damId") Long damId,
            @Param("typeId") Long typeId,
            @Param("sectionId") Long sectionId);
}
