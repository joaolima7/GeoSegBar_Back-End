package com.geosegbar.infra.instrument_graph_pattern.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentGraphPatternEntity;

@Repository
public interface InstrumentGraphPatternRepository extends JpaRepository<InstrumentGraphPatternEntity, Long> {

    List<InstrumentGraphPatternEntity> findByInstrumentId(Long instrumentId);

    Optional<InstrumentGraphPatternEntity> findByNameAndInstrumentId(String name, Long instrumentId);

    boolean existsByNameAndInstrumentId(String name, Long instrumentId);

    @Query("SELECT DISTINCT p FROM InstrumentGraphPatternEntity p "
            + "LEFT JOIN FETCH p.instrument i "
            + "LEFT JOIN FETCH p.folder f "
            + "LEFT JOIN FETCH p.axes a "
            + "LEFT JOIN FETCH p.properties prop "
            + "LEFT JOIN FETCH prop.instrument "
            + "LEFT JOIN FETCH prop.output "
            + "LEFT JOIN FETCH prop.statisticalLimit sl "
            + "LEFT JOIN FETCH sl.output "
            + "LEFT JOIN FETCH prop.deterministicLimit dl "
            + "LEFT JOIN FETCH dl.output "
            + "WHERE p.id = :patternId")
    Optional<InstrumentGraphPatternEntity> findByIdWithAllDetails(@Param("patternId") Long patternId);

    @Query("SELECT DISTINCT p FROM InstrumentGraphPatternEntity p "
            + "LEFT JOIN FETCH p.instrument i "
            + "LEFT JOIN FETCH p.folder f "
            + "LEFT JOIN FETCH p.axes a "
            + "LEFT JOIN FETCH p.properties prop "
            + "LEFT JOIN FETCH prop.instrument "
            + "LEFT JOIN FETCH prop.output "
            + "LEFT JOIN FETCH prop.statisticalLimit sl "
            + "LEFT JOIN FETCH sl.output "
            + "LEFT JOIN FETCH prop.deterministicLimit dl "
            + "LEFT JOIN FETCH dl.output "
            + "WHERE p.instrument.id = :instrumentId")
    List<InstrumentGraphPatternEntity> findByInstrumentIdWithAllDetails(@Param("instrumentId") Long instrumentId);

    List<InstrumentGraphPatternEntity> findByFolderId(Long folderId);

    List<InstrumentGraphPatternEntity> findByFolderIsNull();

    @Query("SELECT p FROM InstrumentGraphPatternEntity p "
            + "WHERE p.instrument.section.id = :sectionId")
    List<InstrumentGraphPatternEntity> findByInstrumentSectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT p FROM InstrumentGraphPatternEntity p "
            + "WHERE p.instrument.dam.id = :damId")
    List<InstrumentGraphPatternEntity> findByInstrumentDamId(@Param("damId") Long damId);

    @Query("SELECT DISTINCT p FROM InstrumentGraphPatternEntity p "
            + "LEFT JOIN FETCH p.instrument i "
            + "LEFT JOIN FETCH i.dam "
            + "LEFT JOIN FETCH p.folder f "
            + "LEFT JOIN FETCH p.axes a "
            + "LEFT JOIN FETCH p.properties prop "
            + "LEFT JOIN FETCH prop.instrument "
            + "LEFT JOIN FETCH prop.output "
            + "LEFT JOIN FETCH prop.statisticalLimit sl "
            + "LEFT JOIN FETCH sl.output "
            + "LEFT JOIN FETCH prop.deterministicLimit dl "
            + "LEFT JOIN FETCH dl.output "
            + "WHERE p.folder.dam.id = :damId "
            + "ORDER BY p.folder.name ASC, p.name ASC")
    List<InstrumentGraphPatternEntity> findByFolderDamIdWithAllDetails(@Param("damId") Long damId);

    @Query("SELECT DISTINCT p FROM InstrumentGraphPatternEntity p "
            + "LEFT JOIN FETCH p.instrument i "
            + "LEFT JOIN FETCH i.dam "
            + "LEFT JOIN FETCH p.folder f "
            + "LEFT JOIN FETCH p.axes a "
            + "LEFT JOIN FETCH p.properties prop "
            + "LEFT JOIN FETCH prop.instrument "
            + "LEFT JOIN FETCH prop.output "
            + "LEFT JOIN FETCH prop.statisticalLimit sl "
            + "LEFT JOIN FETCH sl.output "
            + "LEFT JOIN FETCH prop.deterministicLimit dl "
            + "LEFT JOIN FETCH dl.output "
            + "WHERE p.folder.id = :folderId "
            + "ORDER BY p.name ASC")
    List<InstrumentGraphPatternEntity> findByFolderIdWithAllDetails(@Param("folderId") Long folderId);
}
