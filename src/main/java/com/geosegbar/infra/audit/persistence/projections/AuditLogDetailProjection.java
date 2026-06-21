package com.geosegbar.infra.audit.persistence.projections;

import java.time.LocalDateTime;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.common.enums.RoleEnum;

/**
 * Projeção plana completa (admin/dev). Inclui contexto HTTP, request/response,
 * erro e stack trace. Os dados do ator vêm de LEFT JOIN com a tabela de usuários.
 */
public interface AuditLogDetailProjection {

    Long getId();

    String getAction();

    String getActionLabel();

    AuditSource getSource();

    Long getActorUserId();

    String getActorLabel();

    String getUserName();

    String getUserEmail();

    RoleEnum getUserRole();

    LocalDateTime getOccurredAt();

    AuditStatus getStatus();

    String getMessage();

    Long getDurationMs();

    String getHttpMethod();

    String getEndpoint();

    String getQueryString();

    Integer getHttpStatus();

    String getClientIp();

    String getUserAgent();

    String getOrigin();

    String getRequestBody();

    String getResponseBody();

    String getRequestHeaders();

    String getErrorSummary();

    String getStackTrace();

    String getTraceId();

    String getEntityType();

    Long getEntityId();
}
