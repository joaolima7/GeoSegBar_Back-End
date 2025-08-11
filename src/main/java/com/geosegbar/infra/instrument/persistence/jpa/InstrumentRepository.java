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
import com.geosegbar.entities.SectionEntity;

@Repository
public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {

    List<InstrumentEntity> findAllByOrderByNameAsc();

    List<InstrumentEntity> findByDamId(Long damId);

    List<InstrumentEntity> findByDam(DamEntity dam);

    List<InstrumentEntity> findBySectionId(Long sectionId);

    List<InstrumentEntity> findBySection(SectionEntity section);

    Optional<InstrumentEntity> findByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamIdAndIdNot(String name, Long damId, Long id);

    @Query("SELECT i.id FROM InstrumentEntity i WHERE i.dam.id = :damId")
    List<Long> findInstrumentIdsByDamId(@Param("damId") Long damId);

    @EntityGraph(attributePaths = {"inputs", "constants", "outputs"})
    Optional<InstrumentEntity> findWithIOCById(Long id);

    @EntityGraph(attributePaths = {"inputs", "constants", "outputs", "outputs.statisticalLimit", "outputs.deterministicLimit"})
    @Query("SELECT i FROM InstrumentEntity i LEFT JOIN FETCH i.outputs o WHERE i.id = :id AND (o.active = true OR o IS NULL)")
    Optional<InstrumentEntity> findWithActiveOutputsById(@Param("id") Long id);

    @Query("SELECT i FROM InstrumentEntity i WHERE i.dam.client.id = :clientId")
    List<InstrumentEntity> findByClientId(@Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {"inputs", "constants", "outputs", "outputs.statisticalLimit", "outputs.deterministicLimit"})
    @Query("SELECT i FROM InstrumentEntity i WHERE i.dam.client.id = :clientId AND (:active IS NULL OR i.active = :active)")
    List<InstrumentEntity> findWithAllDetailsByClientId(
            @Param("clientId") Long clientId,
            @Param("active") Boolean active);

    @EntityGraph(attributePaths = {
        "inputs", "inputs.measurementUnit",
        "constants", "constants.measurementUnit",
        "outputs", "outputs.measurementUnit", "outputs.statisticalLimit", "outputs.deterministicLimit",
        "dam", "dam.client", "section"
    })
    @Query("SELECT i FROM InstrumentEntity i WHERE i.id = :id")
    Optional<InstrumentEntity> findWithCompleteDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
        "inputs", "inputs.measurementUnit",
        "constants", "constants.measurementUnit",
        "outputs", "outputs.measurementUnit", "outputs.statisticalLimit", "outputs.deterministicLimit",
        "dam", "dam.client", "section"
    })
    @Query("SELECT i FROM InstrumentEntity i WHERE i.dam.client.id = :clientId AND (:active IS NULL OR i.active = :active)")
    List<InstrumentEntity> findByClientIdOptimized(@Param("clientId") Long clientId, @Param("active") Boolean active);

    @EntityGraph(attributePaths = {
        "inputs", "inputs.measurementUnit",
        "constants", "constants.measurementUnit",
        "outputs", "outputs.measurementUnit", "outputs.statisticalLimit", "outputs.deterministicLimit",
        "dam", "dam.client", "section"
    })
    @Query("SELECT i FROM InstrumentEntity i "
            + "WHERE (:damId IS NULL OR i.dam.id = :damId) "
            + "AND (:instrumentType IS NULL OR i.instrumentType = :instrumentType) "
            + "AND (:sectionId IS NULL OR i.section.id = :sectionId) "
            + "AND (:active IS NULL OR i.active = :active) "
            + "AND (:clientId IS NULL OR i.dam.client.id = :clientId)")
    List<InstrumentEntity> findByFiltersOptimized(
            @Param("damId") Long damId,
            @Param("instrumentType") String instrumentType,
            @Param("sectionId") Long sectionId,
            @Param("active") Boolean active,
            @Param("clientId") Long clientId);
}
