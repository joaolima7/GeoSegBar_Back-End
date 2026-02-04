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

    @Override
    @EntityGraph(attributePaths = {"dam", "dam.client", "section", "instrumentType"})
    Optional<InstrumentEntity> findById(Long id);

    @EntityGraph(attributePaths = {"dam", "dam.client", "section", "instrumentType"})
    List<InstrumentEntity> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"dam", "dam.client", "section", "instrumentType"})
    List<InstrumentEntity> findByDamId(Long damId);

    @EntityGraph(attributePaths = {"instrumentType", "section"})
    List<InstrumentEntity> findByDam(DamEntity dam);

    @EntityGraph(attributePaths = {"dam", "dam.client", "instrumentType"})
    List<InstrumentEntity> findBySectionId(Long sectionId);

    List<InstrumentEntity> findBySection(SectionEntity section);

    Optional<InstrumentEntity> findByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamIdAndIdNot(String name, Long damId, Long id);

    @Query("SELECT i.id FROM InstrumentEntity i WHERE i.dam.id = :damId")
    List<Long> findInstrumentIdsByDamId(@Param("damId") Long damId);

    @EntityGraph(attributePaths = {
        "inputs",
        "constants",
        "outputs",
        "outputs.measurementUnit",
        "outputs.statisticalLimit",
        "outputs.deterministicLimit",
        "dam",
        "instrumentType"
    })
    @Query("SELECT DISTINCT i FROM InstrumentEntity i LEFT JOIN FETCH i.outputs o WHERE i.id = :id AND (o.active = true OR o IS NULL)")
    Optional<InstrumentEntity> findWithActiveOutputsById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT i FROM InstrumentEntity i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH i.section
            LEFT JOIN FETCH i.dam d
            LEFT JOIN FETCH d.client
            WHERE i.id = :id
            """)
    Optional<InstrumentEntity> findByIdWithFullDetails(@Param("id") Long id);

    default Optional<InstrumentEntity> findWithCompleteDetailsById(Long id) {
        return findByIdWithFullDetails(id);
    }

    @Query("SELECT i FROM InstrumentEntity i WHERE i.dam.client.id = :clientId")
    List<InstrumentEntity> findByClientId(@Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {
        "dam", "dam.client", "instrumentType"
    })
    @Query("SELECT DISTINCT i FROM InstrumentEntity i WHERE i.dam.client.id = :clientId AND (:active IS NULL OR i.active = :active)")
    List<InstrumentEntity> findWithAllDetailsByClientId(
            @Param("clientId") Long clientId,
            @Param("active") Boolean active);

    @EntityGraph(attributePaths = {
        "dam", "dam.client", "section", "instrumentType"
    })
    @Query("""
            SELECT DISTINCT i FROM InstrumentEntity i 
            WHERE (:damId IS NULL OR i.dam.id = :damId) 
              AND (:instrumentTypeId IS NULL OR i.instrumentType.id = :instrumentTypeId) 
              AND (:sectionId IS NULL OR i.section.id = :sectionId) 
              AND (:active IS NULL OR i.active = :active) 
              AND (:clientId IS NULL OR i.dam.client.id = :clientId)
              AND (:name IS NULL OR lower(i.name) LIKE lower(concat('%', cast(:name as string), '%')))
            ORDER BY i.name ASC
            """)
    List<InstrumentEntity> findByFiltersOptimized(
            @Param("damId") Long damId,
            @Param("instrumentTypeId") Long instrumentTypeId,
            @Param("sectionId") Long sectionId,
            @Param("active") Boolean active,
            @Param("clientId") Long clientId,
            @Param("name") String name);

    default List<InstrumentEntity> findByFiltersOptimized(Long damId, Long instrumentTypeId, Long sectionId, Boolean active, Long clientId) {
        return findByFiltersOptimized(damId, instrumentTypeId, sectionId, active, clientId, null);
    }

    @EntityGraph(attributePaths = {"dam", "section", "instrumentType"})
    List<InstrumentEntity> findByDamIdAndIsLinimetricRulerTrue(Long damId);

    Optional<InstrumentEntity> findByLinimetricRulerCode(Long linimetricRulerCode);

    boolean existsByLinimetricRulerCodeAndIdNot(Long linimetricRulerCode, Long id);

    @EntityGraph(attributePaths = {"dam", "section", "instrumentType"})
    List<InstrumentEntity> findByIsLinimetricRulerTrue();
}
