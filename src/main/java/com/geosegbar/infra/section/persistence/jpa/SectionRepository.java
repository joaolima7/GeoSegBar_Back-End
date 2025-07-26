package com.geosegbar.infra.section.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.SectionEntity;

@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {

    List<SectionEntity> findAllByOrderByNameAsc();

    Optional<SectionEntity> findByName(String name);

    List<SectionEntity> findAllByDamId(Long damId);

    Optional<SectionEntity> findByDamIdAndName(Long damId, String name);

    @Query("SELECT s FROM SectionEntity s WHERE "
            + "s.firstVertexLatitude = :lat1 AND "
            + "s.secondVertexLatitude = :lat2 AND "
            + "s.firstVertexLongitude = :long1 AND "
            + "s.secondVertexLongitude = :long2")
    List<SectionEntity> findByCoordinates(Double lat1, Double lat2, Double long1, Double long2);
}
