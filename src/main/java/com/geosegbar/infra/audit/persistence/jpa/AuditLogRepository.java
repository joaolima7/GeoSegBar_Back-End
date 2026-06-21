package com.geosegbar.infra.audit.persistence.jpa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AuditLogEntity;
import com.geosegbar.infra.audit.persistence.projections.AuditLogDetailProjection;
import com.geosegbar.infra.audit.persistence.projections.AuditLogSummaryProjection;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * Listagem paginada (visão usuário comum). Os dados do ator são resolvidos via
     * LEFT JOIN com a tabela de usuários — sempre atualizados, sem desnormalização.
     * <p>
     * Filtros opcionais (ignorados quando nulos). Detalhes de robustez no
     * PostgreSQL:
     * <ul>
     *   <li>Todo parâmetro nullable é envolvido em {@code CAST(:p AS tipo)} na
     *       checagem {@code IS NULL}. Sem isso, o Postgres não consegue inferir o
     *       tipo de um {@code ? IS NULL} isolado e falha com
     *       "could not determine data type of parameter".</li>
     *   <li>Os filtros de texto por substring recebem o padrão LIKE já montado em
     *       minúsculas ({@code %valor%}) pela camada de serviço; o {@code LOWER()}
     *       incide sobre a COLUNA (nunca sobre o parâmetro), evitando o erro
     *       "function lower(bytea) does not exist".</li>
     * </ul>
     */
    @Query(value = """
            SELECT a.id AS id,
                   a.occurredAt AS occurredAt,
                   a.actorUserId AS actorUserId,
                   a.actorLabel AS actorLabel,
                   u.name AS userName,
                   u.email AS userEmail,
                   r.name AS userRole,
                   a.action AS action,
                   a.actionLabel AS actionLabel,
                   a.source AS source,
                   a.status AS status,
                   a.message AS message
            FROM AuditLogEntity a
            LEFT JOIN UserEntity u ON u.id = a.actorUserId
            LEFT JOIN u.role r
            WHERE (CAST(:startDate AS timestamp) IS NULL OR a.occurredAt >= :startDate)
              AND (CAST(:endDate AS timestamp) IS NULL OR a.occurredAt <= :endDate)
              AND (CAST(:actorUserId AS long) IS NULL OR a.actorUserId = :actorUserId)
              AND (CAST(:actorEmailPattern AS string) IS NULL OR LOWER(u.email) LIKE :actorEmailPattern)
              AND (CAST(:actionPattern AS string) IS NULL OR LOWER(a.action) LIKE :actionPattern)
              AND (CAST(:status AS string) IS NULL OR CAST(a.status AS string) = :status)
              AND (CAST(:source AS string) IS NULL OR CAST(a.source AS string) = :source)
              AND (CAST(:httpMethod AS string) IS NULL OR a.httpMethod = :httpMethod)
              AND (CAST(:entityType AS string) IS NULL OR a.entityType = :entityType)
              AND (CAST(:entityId AS long) IS NULL OR a.entityId = :entityId)
            """,
            countQuery = """
            SELECT COUNT(a)
            FROM AuditLogEntity a
            WHERE (CAST(:startDate AS timestamp) IS NULL OR a.occurredAt >= :startDate)
              AND (CAST(:endDate AS timestamp) IS NULL OR a.occurredAt <= :endDate)
              AND (CAST(:actorUserId AS long) IS NULL OR a.actorUserId = :actorUserId)
              AND (CAST(:actorEmailPattern AS string) IS NULL OR a.actorUserId IN (SELECT u.id FROM UserEntity u WHERE LOWER(u.email) LIKE :actorEmailPattern))
              AND (CAST(:actionPattern AS string) IS NULL OR LOWER(a.action) LIKE :actionPattern)
              AND (CAST(:status AS string) IS NULL OR CAST(a.status AS string) = :status)
              AND (CAST(:source AS string) IS NULL OR CAST(a.source AS string) = :source)
              AND (CAST(:httpMethod AS string) IS NULL OR a.httpMethod = :httpMethod)
              AND (CAST(:entityType AS string) IS NULL OR a.entityType = :entityType)
              AND (CAST(:entityId AS long) IS NULL OR a.entityId = :entityId)
            """)
    Page<AuditLogSummaryProjection> findSummaries(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("actorUserId") Long actorUserId,
            @Param("actorEmailPattern") String actorEmailPattern,
            @Param("actionPattern") String actionPattern,
            @Param("status") String status,
            @Param("source") String source,
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
                   r.name AS userRole,
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
            LEFT JOIN u.role r
            WHERE (CAST(:startDate AS timestamp) IS NULL OR a.occurredAt >= :startDate)
              AND (CAST(:endDate AS timestamp) IS NULL OR a.occurredAt <= :endDate)
              AND (CAST(:actorUserId AS long) IS NULL OR a.actorUserId = :actorUserId)
              AND (CAST(:actorEmailPattern AS string) IS NULL OR LOWER(u.email) LIKE :actorEmailPattern)
              AND (CAST(:actionPattern AS string) IS NULL OR LOWER(a.action) LIKE :actionPattern)
              AND (CAST(:status AS string) IS NULL OR CAST(a.status AS string) = :status)
              AND (CAST(:source AS string) IS NULL OR CAST(a.source AS string) = :source)
              AND (CAST(:httpMethod AS string) IS NULL OR a.httpMethod = :httpMethod)
              AND (CAST(:entityType AS string) IS NULL OR a.entityType = :entityType)
              AND (CAST(:entityId AS long) IS NULL OR a.entityId = :entityId)
            """,
            countQuery = """
            SELECT COUNT(a)
            FROM AuditLogEntity a
            WHERE (CAST(:startDate AS timestamp) IS NULL OR a.occurredAt >= :startDate)
              AND (CAST(:endDate AS timestamp) IS NULL OR a.occurredAt <= :endDate)
              AND (CAST(:actorUserId AS long) IS NULL OR a.actorUserId = :actorUserId)
              AND (CAST(:actorEmailPattern AS string) IS NULL OR a.actorUserId IN (SELECT u.id FROM UserEntity u WHERE LOWER(u.email) LIKE :actorEmailPattern))
              AND (CAST(:actionPattern AS string) IS NULL OR LOWER(a.action) LIKE :actionPattern)
              AND (CAST(:status AS string) IS NULL OR CAST(a.status AS string) = :status)
              AND (CAST(:source AS string) IS NULL OR CAST(a.source AS string) = :source)
              AND (CAST(:httpMethod AS string) IS NULL OR a.httpMethod = :httpMethod)
              AND (CAST(:entityType AS string) IS NULL OR a.entityType = :entityType)
              AND (CAST(:entityId AS long) IS NULL OR a.entityId = :entityId)
            """)
    Page<AuditLogDetailProjection> findDetails(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("actorUserId") Long actorUserId,
            @Param("actorEmailPattern") String actorEmailPattern,
            @Param("actionPattern") String actionPattern,
            @Param("status") String status,
            @Param("source") String source,
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
                   r.name AS userRole,
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
            LEFT JOIN u.role r
            WHERE a.id = :id
            """)
    Optional<AuditLogDetailProjection> findDetailById(@Param("id") Long id);
}
