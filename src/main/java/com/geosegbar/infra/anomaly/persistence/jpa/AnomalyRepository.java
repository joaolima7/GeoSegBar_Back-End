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
import com.geosegbar.infra.dashboard.projections.RecentAnomalyProjection;

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

    @Query(value = """
            SELECT
                a.id as id,
                u.id as userId,
                u.name as userName,
                d.id as damId,
                d.name as damName,
                a.created_at as createdAt,
                a.latitude as latitude,
                a.longitude as longitude,
                a.origin as origin,
                a.observation as observation,
                a.recommendation as recommendation,
                dl.name as dangerLevelName,
                ast.name as statusName
            FROM anomalies a
            INNER JOIN users u ON a.user_id = u.id
            INNER JOIN dam d ON a.dam_id = d.id
            INNER JOIN danger_levels dl ON a.danger_level_id = dl.id
            INNER JOIN anomaly_status ast ON a.status_id = ast.id
            WHERE a.dam_id IN (:damIds)
            ORDER BY a.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<RecentAnomalyProjection> findRecentByDamIds(
            @Param("damIds") List<Long> damIds,
            @Param("limit") int limit);
}
