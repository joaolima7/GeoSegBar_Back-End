package com.geosegbar.infra.section.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.SectionEntity;

@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {

    // -------------------------------------------------------
    // Queries para getSectionsByFilters — withDam=false
    // dam é LAZY: não carregada, máxima performance
    // -------------------------------------------------------

    @Query("SELECT s FROM SectionEntity s ORDER BY s.name ASC")
    List<SectionEntity> findAllOrdered();

    @Query("SELECT s FROM SectionEntity s WHERE s.dam.id = :damId ORDER BY s.name ASC")
    List<SectionEntity> findByDamId(@Param("damId") Long damId);

    // Alias mantido para compatibilidade com BulkInstrumentImportService
    default List<SectionEntity> findAllByDamId(Long damId) {
        return findByDamId(damId);
    }

    @Query("SELECT s FROM SectionEntity s WHERE s.id = :id")
    Optional<SectionEntity> findByIdNoDam(@Param("id") Long id);

    // -------------------------------------------------------
    // Queries para getSectionsByFilters — withDam=true
    // JOIN FETCH dam: uma única query, sem N+1
    // -------------------------------------------------------

    @Query("SELECT s FROM SectionEntity s LEFT JOIN FETCH s.dam ORDER BY s.name ASC")
    List<SectionEntity> findAllOrderedWithDam();

    @Query("SELECT s FROM SectionEntity s LEFT JOIN FETCH s.dam WHERE s.dam.id = :damId ORDER BY s.name ASC")
    List<SectionEntity> findByDamIdWithDam(@Param("damId") Long damId);

    // findById com EntityGraph serve tanto para withDam=true+sectionId
    // quanto para operações internas (update/delete) que precisam da dam
    @Override
    @EntityGraph(attributePaths = {"dam"})
    Optional<SectionEntity> findById(Long id);

    // -------------------------------------------------------
    // Uso interno: validação de duplicidade em create/update
    // -------------------------------------------------------

    @EntityGraph(attributePaths = {"dam"})
    Optional<SectionEntity> findByName(String name);

    @EntityGraph(attributePaths = {"dam"})
    Optional<SectionEntity> findByDamIdAndName(Long damId, String name);

    // -------------------------------------------------------
    // Uso interno: delete (precisa carregar instruments)
    // -------------------------------------------------------

    @EntityGraph(attributePaths = {"dam", "instruments"})
    @Query("SELECT s FROM SectionEntity s WHERE s.id = :id")
    Optional<SectionEntity> findByIdWithInstruments(@Param("id") Long id);

    // -------------------------------------------------------
    // Consulta por coordenadas (uso específico)
    // -------------------------------------------------------

    @Query("SELECT s FROM SectionEntity s WHERE "
            + "s.firstVertexLatitude = :lat1 AND "
            + "s.secondVertexLatitude = :lat2 AND "
            + "s.firstVertexLongitude = :long1 AND "
            + "s.secondVertexLongitude = :long2")
    @EntityGraph(attributePaths = {"dam"})
    List<SectionEntity> findByCoordinates(@Param("lat1") Double lat1, @Param("lat2") Double lat2,
            @Param("long1") Double long1, @Param("long2") Double long2);
}
