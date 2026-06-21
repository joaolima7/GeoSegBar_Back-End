package com.geosegbar.infra.audit.dtos;

import java.time.LocalDateTime;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;

import lombok.Getter;
import lombok.Setter;

/**
 * Filtros aceitos pelos endpoints de consulta de auditoria. Todos opcionais.
 */
@Getter
@Setter
public class AuditLogFilterDTO {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long actorUserId;
    private String actorEmail;
    private String action;
    private AuditStatus status;
    private AuditSource source;
    private String httpMethod;
    private String entityType;
    private Long entityId;
}
