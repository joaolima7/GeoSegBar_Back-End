package com.geosegbar.infra.audit.dtos;

import java.time.LocalDateTime;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.infra.audit.persistence.projections.AuditLogDetailProjection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Visão de auditoria para admin/dev: todos os campos, incluindo contexto HTTP,
 * request/response (presentes apenas em erro), resumo do erro e stack trace. Os
 * dados do ator vêm do JOIN com a tabela de usuários (sempre atualizados); para
 * atores sem conta, usa-se o rótulo de fallback.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDetailDTO {

    private Long id;
    private String action;
    private String actionLabel;
    private AuditSource source;

    private Long actorUserId;
    private String actorName;
    private String actorEmail;
    private String actorRole;

    private LocalDateTime occurredAt;
    private AuditStatus status;
    private String message;
    private Long durationMs;

    private String httpMethod;
    private String endpoint;
    private String queryString;
    private Integer httpStatus;
    private String clientIp;
    private String userAgent;
    private String origin;

    private String requestBody;
    private String responseBody;
    private String requestHeaders;
    private String errorSummary;
    private String stackTrace;

    private String traceId;
    private String entityType;
    private Long entityId;

    public static AuditLogDetailDTO fromProjection(AuditLogDetailProjection p) {
        return AuditLogDetailDTO.builder()
                .id(p.getId())
                .action(p.getAction())
                .actionLabel(p.getActionLabel())
                .source(p.getSource())
                .actorUserId(p.getActorUserId())
                .actorName(AuditActorResolver.resolveName(p.getUserName(), p.getActorLabel(), p.getActorUserId()))
                .actorEmail(p.getUserEmail())
                .actorRole(p.getUserRole() != null ? p.getUserRole().name() : null)
                .occurredAt(p.getOccurredAt())
                .status(p.getStatus())
                .message(p.getMessage())
                .durationMs(p.getDurationMs())
                .httpMethod(p.getHttpMethod())
                .endpoint(p.getEndpoint())
                .queryString(p.getQueryString())
                .httpStatus(p.getHttpStatus())
                .clientIp(p.getClientIp())
                .userAgent(p.getUserAgent())
                .origin(p.getOrigin())
                .requestBody(p.getRequestBody())
                .responseBody(p.getResponseBody())
                .requestHeaders(p.getRequestHeaders())
                .errorSummary(p.getErrorSummary())
                .stackTrace(p.getStackTrace())
                .traceId(p.getTraceId())
                .entityType(p.getEntityType())
                .entityId(p.getEntityId())
                .build();
    }
}
