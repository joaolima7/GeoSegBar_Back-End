package com.geosegbar.infra.audit.events;

import com.geosegbar.entities.AuditLogEntity;

import lombok.Getter;

/**
 * Evento de aplicação que carrega um {@link AuditLogEntity} já montado e pronto
 * para persistir. Publicado pelo {@code AuditService} e consumido de forma
 * assíncrona pelo {@code AuditPersistenceListener}.
 */
@Getter
public class AuditEvent {

    private final AuditLogEntity auditLog;

    public AuditEvent(AuditLogEntity auditLog) {
        this.auditLog = auditLog;
    }
}
