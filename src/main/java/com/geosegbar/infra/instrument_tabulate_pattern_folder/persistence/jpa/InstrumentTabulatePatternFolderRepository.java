package com.geosegbar.infra.instrument_tabulate_pattern_folder.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentTabulatePatternFolder;

@Repository
public interface InstrumentTabulatePatternFolderRepository extends JpaRepository<InstrumentTabulatePatternFolder, Long> {

    Optional<InstrumentTabulatePatternFolder> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndDamId(String name, Long damId);

    List<InstrumentTabulatePatternFolder> findAllByOrderByNameAsc();

    List<InstrumentTabulatePatternFolder> findByDamId(Long damId);

    List<InstrumentTabulatePatternFolder> findByDamIdOrderByNameAsc(Long damId);

    @Query("SELECT f FROM InstrumentTabulatePatternFolder f WHERE f.name LIKE %:name%")
    List<InstrumentTabulatePatternFolder> findByNameContaining(@Param("name") String name);

    @Query("SELECT f FROM InstrumentTabulatePatternFolder f "
            + "WHERE f.dam.id = :damId AND f.name LIKE %:name%")
    List<InstrumentTabulatePatternFolder> findByDamIdAndNameContaining(@Param("damId") Long damId, @Param("name") String name);

    @Query("SELECT DISTINCT f FROM InstrumentTabulatePatternFolder f "
            + "JOIN f.patterns p "
            + "JOIN p.associations a "
            + "WHERE a.instrument.id = :instrumentId")
    List<InstrumentTabulatePatternFolder> findByPatternInstrumentId(@Param("instrumentId") Long instrumentId);

    @Query("SELECT f FROM InstrumentTabulatePatternFolder f "
            + "LEFT JOIN FETCH f.dam d "
            + "WHERE f.id = :folderId")
    Optional<InstrumentTabulatePatternFolder> findByIdWithDam(@Param("folderId") Long folderId);

    @Query("SELECT f FROM InstrumentTabulatePatternFolder f "
            + "LEFT JOIN FETCH f.dam d "
            + "WHERE f.dam.id = :damId "
            + "ORDER BY f.name ASC")
    List<InstrumentTabulatePatternFolder> findByDamIdWithDamDetails(@Param("damId") Long damId);
}
