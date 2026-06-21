package com.geosegbar.infra.audit.persistence.jpa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.entities.AuditLogEntity;
import com.geosegbar.infra.audit.persistence.projections.AuditLogDetailProjection;
import com.geosegbar.infra.audit.persistence.projections.AuditLogSummaryProjection;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * Listagem paginada (visão usuário comum). Os dados do ator são resolvidos via
     * LEFT JOIN com a tabela de usuários — sempre atualizados, sem desnormalização.
     * Todos os filtros são opcionais (ignorados quando nulos).
     */
    @Query(value = """
            SELECT a.id AS id,
                   a.occurredAt AS occurredAt,
                   a.actorUserId AS actorUserId,
                   a.actorLabel AS actorLabel,
                   u.name AS userName,
                   u.email AS userEmail,
                   u.role.name AS userRole,
                   a.action AS action,
                   a.actionLabel AS actionLabel,
                   a.source AS source,
                   a.status AS status,
                   a.message AS message
            FROM AuditLogEntity a
            LEFT JOIN UserEntity u ON u.id = a.actorUserId
            WHERE (:startDate IS NULL OR a.occurredAt >= :startDate)
              AND (:endDate IS NULL OR a.occurredAt <= :endDate)
              AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
              AND (:actorEmail IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :actorEmail, '%')))
              AND (:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%')))
              AND (:status IS NULL OR a.status = :status)
              AND (:source IS NULL OR a.source = :source)
              AND (:httpMethod IS NULL OR UPPER(a.httpMethod) = UPPER(:httpMethod))
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
            """,
            countQuery = """
            SELECT COUNT(a)
            FROM AuditLogEntity a
            LEFT JOIN UserEntity u ON u.id = a.actorUserId
            WHERE (:startDate IS NULL OR a.occurredAt >= :startDate)
              AND (:endDate IS NULL OR a.occurredAt <= :endDate)
              AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
              AND (:actorEmail IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :actorEmail, '%')))
              AND (:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%')))
              AND (:status IS NULL OR a.status = :status)
              AND (:source IS NULL OR a.source = :source)
              AND (:httpMethod IS NULL OR UPPER(a.httpMethod) = UPPER(:httpMethod))
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
            """)
    Page<AuditLogSummaryProjection> findSummaries(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("actorUserId") Long actorUserId,
            @Param("actorEmail") String actorEmail,
            @Param("action") String action,
            @Param("status") AuditStatus status,
            @Param("source") AuditSource source,
            @Param("httpMethod") String httpMethod,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            Pageable pageable);

    /**
     * Listagem paginada (visão admin/dev) com todos os campos.
     */
    @Query(value = """
            SELECT a.id AS id,
                   a.action AS action,
                   a.actionLabel AS actionLabel,
                   a.source AS source,
                   a.actorUserId AS actorUserId,
                   a.actorLabel AS actorLabel,
                   u.name AS userName,
                   u.email AS userEmail,
                   u.role.name AS userRole,
                   a.occurredAt AS occurredAt,
                   a.status AS status,
                   a.message AS message,
                   a.durationMs AS durationMs,
                   a.httpMethod AS httpMethod,
                   a.endpoint AS endpoint,
                   a.queryString AS queryString,
                   a.httpStatus AS httpStatus,
                   a.clientIp AS clientIp,
                   a.userAgent AS userAgent,
                   a.origin AS origin,
                   a.requestBody AS requestBody,
                   a.responseBody AS responseBody,
                   a.requestHeaders AS requestHeaders,
                   a.errorSummary AS errorSummary,
                   a.stackTrace AS stackTrace,
                   a.traceId AS traceId,
                   a.entityType AS entityType,
                   a.entityId AS entityId
            FROM AuditLogEntity a
            LEFT JOIN UserEntity u ON u.id = a.actorUserId
            WHERE (:startDate IS NULL OR a.occurredAt >= :startDate)
              AND (:endDate IS NULL OR a.occurredAt <= :endDate)
              AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
              AND (:actorEmail IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :actorEmail, '%')))
              AND (:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%')))
              AND (:status IS NULL OR a.status = :status)
              AND (:source IS NULL OR a.source = :source)
              AND (:httpMethod IS NULL OR UPPER(a.httpMethod) = UPPER(:httpMethod))
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
            """,
            countQuery = """
            SELECT COUNT(a)
            FROM AuditLogEntity a
            LEFT JOIN UserEntity u ON u.id = a.actorUserId
            WHERE (:startDate IS NULL OR a.occurredAt >= :startDate)
              AND (:endDate IS NULL OR a.occurredAt <= :endDate)
              AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
              AND (:actorEmail IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :actorEmail, '%')))
              AND (:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%')))
              AND (:status IS NULL OR a.status = :status)
              AND (:source IS NULL OR a.source = :source)
              AND (:httpMethod IS NULL OR UPPER(a.httpMethod) = UPPER(:httpMethod))
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
            """)
    Page<AuditLogDetailProjection> findDetails(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("actorUserId") Long actorUserId,
            @Param("actorEmail") String actorEmail,
            @Param("action") String action,
            @Param("status") AuditStatus status,
            @Param("source") AuditSource source,
            @Param("httpMethod") String httpMethod,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            Pageable pageable);

    /**
     * Detalhe completo de um único registro (admin/dev).
     */
    @Query("""
            SELECT a.id AS id,
                   a.action AS action,
                   a.actionLabel AS actionLabel,
                   a.source AS source,
                   a.actorUserId AS actorUserId,
                   a.actorLabel AS actorLabel,
                   u.name AS userName,
                   u.email AS userEmail,
                   u.role.name AS userRole,
                   a.occurredAt AS occurredAt,
                   a.status AS status,
                   a.message AS message,
                   a.durationMs AS durationMs,
                   a.httpMethod AS httpMethod,
                   a.endpoint AS endpoint,
                   a.queryString AS queryString,
                   a.httpStatus AS httpStatus,
                   a.clientIp AS clientIp,
                   a.userAgent AS userAgent,
                   a.origin AS origin,
                   a.requestBody AS requestBody,
                   a.responseBody AS responseBody,
                   a.requestHeaders AS requestHeaders,
                   a.errorSummary AS errorSummary,
                   a.stackTrace AS stackTrace,
                   a.traceId AS traceId,
                   a.entityType AS entityType,
                   a.entityId AS entityId
            FROM AuditLogEntity a
            LEFT JOIN UserEntity u ON u.id = a.actorUserId
            WHERE a.id = :id
            """)
    Optional<AuditLogDetailProjection> findDetailById(@Param("id") Long id);
}
