package com.geosegbar.infra.instrument_graph_pattern_folder.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentGraphPatternFolder;

@Repository
public interface InstrumentGraphPatternFolderRepository extends JpaRepository<InstrumentGraphPatternFolder, Long> {

    Optional<InstrumentGraphPatternFolder> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndDamId(String name, Long damId);

    @EntityGraph(attributePaths = {"dam"})
    List<InstrumentGraphPatternFolder> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"dam"})
    List<InstrumentGraphPatternFolder> findByDamId(Long damId);

    @EntityGraph(attributePaths = {"dam"})
    List<InstrumentGraphPatternFolder> findByDamIdOrderByNameAsc(Long damId);

    @Query("SELECT f FROM InstrumentGraphPatternFolder f WHERE f.name LIKE %:name%")
    @EntityGraph(attributePaths = {"dam"})
    List<InstrumentGraphPatternFolder> findByNameContaining(@Param("name") String name);

    @Query("SELECT f FROM InstrumentGraphPatternFolder f "
            + "WHERE f.dam.id = :damId AND f.name LIKE %:name%")
    @EntityGraph(attributePaths = {"dam"})
    List<InstrumentGraphPatternFolder> findByDamIdAndNameContaining(@Param("damId") Long damId, @Param("name") String name);

    @Query("SELECT DISTINCT f FROM InstrumentGraphPatternFolder f "
            + "JOIN f.patterns p "
            + "LEFT JOIN FETCH f.dam "
            + "WHERE p.instrument.id = :instrumentId")
    List<InstrumentGraphPatternFolder> findByPatternInstrumentId(@Param("instrumentId") Long instrumentId);

    @Query("SELECT f FROM InstrumentGraphPatternFolder f "
            + "LEFT JOIN FETCH f.dam d "
            + "WHERE f.id = :folderId")
    Optional<InstrumentGraphPatternFolder> findByIdWithDam(@Param("folderId") Long folderId);

    @Override
    @EntityGraph(attributePaths = {"dam"})
    Optional<InstrumentGraphPatternFolder> findById(Long id);

    @Query("SELECT f FROM InstrumentGraphPatternFolder f "
            + "LEFT JOIN FETCH f.dam d "
            + "WHERE f.dam.id = :damId "
            + "ORDER BY f.name ASC")
    List<InstrumentGraphPatternFolder> findByDamIdWithDamDetails(@Param("damId") Long damId);
}
