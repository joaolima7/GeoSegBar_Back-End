package com.geosegbar.infra.instrument_tabulate_pattern.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentTabulatePatternEntity;

import jakarta.persistence.QueryHint;

@Repository
public interface InstrumentTabulatePatternRepository extends JpaRepository<InstrumentTabulatePatternEntity, Long> {

    @EntityGraph(attributePaths = {"dam", "folder"})
    List<InstrumentTabulatePatternEntity> findByDamId(Long damId);

    @EntityGraph(attributePaths = {"dam", "folder"})
    List<InstrumentTabulatePatternEntity> findByFolderId(Long folderId);

    @EntityGraph(attributePaths = {"dam", "folder"})
    List<InstrumentTabulatePatternEntity> findByDamIdOrderByNameAsc(Long damId);

    boolean existsByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamIdAndIdNot(String name, Long damId, Long id);

    @Query("SELECT p FROM InstrumentTabulatePatternEntity p "
            + "WHERE p.dam.id = :damId AND p.folder IS NULL "
            + "ORDER BY p.name ASC")
    @EntityGraph(attributePaths = {"dam"})
    List<InstrumentTabulatePatternEntity> findByDamIdWithoutFolder(@Param("damId") Long damId);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT DISTINCT p FROM InstrumentTabulatePatternEntity p "
            + "LEFT JOIN FETCH p.dam "
            + "LEFT JOIN FETCH p.folder "
            + "LEFT JOIN FETCH p.associations a "
            + "LEFT JOIN FETCH a.instrument i "
            + "LEFT JOIN FETCH a.outputAssociations oa "
            + "LEFT JOIN FETCH oa.output o "
            + "LEFT JOIN FETCH o.measurementUnit "
            + "WHERE p.folder.dam.id = :damId "
            + "ORDER BY p.name ASC")
    List<InstrumentTabulatePatternEntity> findByFolderDamIdWithAllDetails(@Param("damId") Long damId);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT DISTINCT p FROM InstrumentTabulatePatternEntity p "
            + "LEFT JOIN FETCH p.dam "
            + "LEFT JOIN FETCH p.associations a "
            + "LEFT JOIN FETCH a.instrument i "
            + "LEFT JOIN FETCH a.outputAssociations oa "
            + "LEFT JOIN FETCH oa.output o "
            + "LEFT JOIN FETCH o.measurementUnit "
            + "WHERE p.dam.id = :damId "
            + "AND p.folder IS NULL "
            + "ORDER BY p.name ASC")
    List<InstrumentTabulatePatternEntity> findByDamIdWithoutFolderWithAllDetails(@Param("damId") Long damId);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT DISTINCT p FROM InstrumentTabulatePatternEntity p "
            + "LEFT JOIN FETCH p.dam "
            + "LEFT JOIN FETCH p.folder "
            + "LEFT JOIN FETCH p.associations a "
            + "LEFT JOIN FETCH a.instrument i "
            + "LEFT JOIN FETCH a.outputAssociations oa "
            + "LEFT JOIN FETCH oa.output o "
            + "LEFT JOIN FETCH o.measurementUnit "
            + "WHERE p.id = :patternId")
    Optional<InstrumentTabulatePatternEntity> findByIdWithAllDetails(@Param("patternId") Long patternId);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT DISTINCT p FROM InstrumentTabulatePatternEntity p "
            + "LEFT JOIN FETCH p.dam "
            + "LEFT JOIN FETCH p.folder "
            + "WHERE p.id = :patternId")
    Optional<InstrumentTabulatePatternEntity> findByIdWithBasicDetails(@Param("patternId") Long patternId);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT DISTINCT p FROM InstrumentTabulatePatternEntity p "
            + "LEFT JOIN FETCH p.dam "
            + "LEFT JOIN FETCH p.folder "
            + "LEFT JOIN FETCH p.associations a "
            + "LEFT JOIN FETCH a.instrument i "
            + "LEFT JOIN FETCH a.outputAssociations oa "
            + "LEFT JOIN FETCH oa.output o "
            + "LEFT JOIN FETCH o.measurementUnit "
            + "WHERE p.dam.id = :damId "
            + "ORDER BY p.name ASC")
    List<InstrumentTabulatePatternEntity> findByDamIdWithAllDetails(@Param("damId") Long damId);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT DISTINCT p FROM InstrumentTabulatePatternEntity p "
            + "LEFT JOIN FETCH p.dam "
            + "LEFT JOIN FETCH p.folder "
            + "LEFT JOIN FETCH p.associations a "
            + "LEFT JOIN FETCH a.instrument i "
            + "LEFT JOIN FETCH a.outputAssociations oa "
            + "LEFT JOIN FETCH oa.output o "
            + "LEFT JOIN FETCH o.measurementUnit "
            + "WHERE p.folder.id = :folderId "
            + "ORDER BY p.name ASC")
    List<InstrumentTabulatePatternEntity> findByFolderIdWithAllDetails(@Param("folderId") Long folderId);
}
