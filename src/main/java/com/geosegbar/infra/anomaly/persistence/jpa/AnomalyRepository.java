package com.geosegbar.infra.anomaly.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.infra.dashboard.projections.CategoryCountProjection;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {

    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findByDamIdAndUserId(Long damId, Long userId);

    @Override
    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    Optional<AnomalyEntity> findById(Long id);

    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findByDamId(Long damId);

    // ===================== Dashboard Queries =====================
    @Query("SELECT dl.name AS name, COUNT(a) AS count "
            + "FROM AnomalyEntity a JOIN a.dangerLevel dl "
            + "WHERE a.dam.id IN :damIds "
            + "AND a.createdAt >= :startDate "
            + "AND a.createdAt <= :endDate "
            + "GROUP BY dl.name")
    List<CategoryCountProjection> countByDangerLevelGrouped(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s.name AS name, COUNT(a) AS count "
            + "FROM AnomalyEntity a JOIN a.status s "
            + "WHERE a.dam.id IN :damIds "
            + "AND a.createdAt >= :startDate "
            + "AND a.createdAt <= :endDate "
            + "GROUP BY s.name")
    List<CategoryCountProjection> countByStatusGrouped(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
