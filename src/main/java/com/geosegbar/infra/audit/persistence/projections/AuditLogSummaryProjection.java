package com.geosegbar.infra.audit.persistence.projections;

import java.time.LocalDateTime;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.common.enums.RoleEnum;

/**
 * Projeção plana da listagem de auditoria para o usuário comum. Os dados do ator
 * ({@code userName}, {@code userEmail}, {@code userRole}) vêm de um LEFT JOIN com
 * a tabela de usuários — sempre atualizados, sem desnormalização.
 */
public interface AuditLogSummaryProjection {

    Long getId();

    LocalDateTime getOccurredAt();

    Long getActorUserId();

    String getActorLabel();

    String getUserName();

    String getUserEmail();

    RoleEnum getUserRole();

    String getAction();

    String getActionLabel();

    AuditSource getSource();

    AuditStatus getStatus();

    String getMessage();
}
