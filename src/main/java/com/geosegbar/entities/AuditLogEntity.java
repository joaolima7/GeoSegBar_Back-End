package com.geosegbar.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de auditoria do sistema. Cada linha representa uma ação ocorrida —
 * via requisição HTTP ou via job/tarefa em background — indicando quem fez,
 * quando, qual ação e o resultado (sucesso/erro) com a respectiva mensagem.
 * <p>
 * Campos de request/response/stacktrace são preenchidos apenas em caso de erro,
 * para facilitar a depuração. Em sucesso, somente metadados + mensagem.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_log_occurred_at", columnList = "occurred_at"),
    @Index(name = "idx_audit_log_actor_user_id", columnList = "actor_user_id"),
    @Index(name = "idx_audit_log_action_occurred", columnList = "action, occurred_at"),
    @Index(name = "idx_audit_log_status", columnList = "status"),
    @Index(name = "idx_audit_log_source", columnList = "source"),
    @Index(name = "idx_audit_log_http_method", columnList = "http_method"),
    @Index(name = "idx_audit_log_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_log_trace_id", columnList = "trace_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- Identidade / ação ----
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "action_label", length = 150)
    private String actionLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private AuditSource source;

    // ---- Quem ----
    // Guardamos apenas a FK do usuário. Nome/e-mail/role NÃO são desnormalizados:
    // são resolvidos por JOIN na leitura (sempre atualizados, sem duplicação).
    @Column(name = "actor_user_id")
    private Long actorUserId;

    // Rótulo textual de fallback para atores SEM conta de usuário:
    // e-mail tentado em login que falhou, "Sistema (Job)", "Anônimo/Não autenticado".
    // Para ações de usuário autenticado fica null (os dados vêm do JOIN).
    @Column(name = "actor_label", length = 255)
    private String actorLabel;

    // ---- Quando / resultado ----
    @Column(name = "occurred_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditStatus status;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "duration_ms")
    private Long durationMs;

    // ---- Contexto HTTP (nullable para jobs) ----
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "query_string", length = 1000)
    private String queryString;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "origin", length = 512)
    private String origin;

    // ---- Detalhe (admin/dev) — preenchido apenas em erro para os bodies ----
    @Column(name = "request_body", columnDefinition = "text")
    private String requestBody;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "request_headers", columnDefinition = "text")
    private String requestHeaders;

    @Column(name = "error_summary", length = 2000)
    private String errorSummary;

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    // ---- Correlação ----
    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;
}
