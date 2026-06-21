package com.geosegbar.infra.audit.services;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Dados de uma ação a ser auditada. Montado pelo filtro HTTP (caminho
 * automático) ou pelos jobs/serviços async (caminho programático) e entregue ao
 * {@link AuditService}.
 * <p>
 * Todos os campos são valores simples (String/Long/Integer), copiados no thread
 * de origem — assim a persistência assíncrona não depende de proxies lazy nem do
 * {@code SecurityContext}.
 */
@Getter
@Builder
public class AuditContext {

    // Identidade / ação
    private final String action;
    private final String actionLabel;
    private final AuditSource source;

    // Quem. Grava-se apenas a FK do usuário (dados resolvidos por JOIN na leitura).
    // actorUserId: usuário autenticado conhecido (opcional — senão resolvido do SecurityContext).
    // actorLabel: rótulo de fallback para atores SEM conta (e-mail de login que falhou, "Sistema (Job)").
    private final Long actorUserId;
    private final String actorLabel;

    // Resultado
    private final AuditStatus status;
    private final String message;
    private final Long durationMs;

    // Contexto HTTP
    private final String httpMethod;
    private final String endpoint;
    private final String queryString;
    private final Integer httpStatus;
    private final String clientIp;
    private final String userAgent;
    private final String origin;

    // Detalhe (erro)
    private final String requestBody;
    private final String responseBody;
    private final String requestHeaders;
    private final Throwable error;
    private final String errorSummary;

    // Correlação
    private final String traceId;
    private final String entityType;
    private final Long entityId;
}
