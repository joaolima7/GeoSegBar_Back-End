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

    @EntityGraph(attributePaths = {"dam"})
    List<SectionEntity> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"dam"})
    Optional<SectionEntity> findByName(String name);

    @EntityGraph(attributePaths = {"dam"})
    List<SectionEntity> findAllByDamId(Long damId);

    @EntityGraph(attributePaths = {"dam"})
    Optional<SectionEntity> findByDamIdAndName(Long damId, String name);

    @Query("SELECT s FROM SectionEntity s WHERE "
            + "s.firstVertexLatitude = :lat1 AND "
            + "s.secondVertexLatitude = :lat2 AND "
            + "s.firstVertexLongitude = :long1 AND "
            + "s.secondVertexLongitude = :long2")
    @EntityGraph(attributePaths = {"dam"})
    List<SectionEntity> findByCoordinates(@Param("lat1") Double lat1, @Param("lat2") Double lat2,
            @Param("long1") Double long1, @Param("long2") Double long2);

    @Override
    @EntityGraph(attributePaths = {"dam"})
    Optional<SectionEntity> findById(Long id);

    @EntityGraph(attributePaths = {"dam", "instruments"})
    @Query("SELECT s FROM SectionEntity s WHERE s.id = :id")
    Optional<SectionEntity> findByIdWithInstruments(@Param("id") Long id);
}
