package com.geosegbar.common.enums;

/**
 * Origem de um registro de auditoria.
 * <p>
 * HTTP: capturado automaticamente pelo filtro a partir de uma requisição.
 * SCHEDULED: jobs disparados por {@code @Scheduled}.
 * ASYNC: tarefas em background ({@code @Async} / event listeners) fora de uma requisição.
 * JOB: processamento de jobs de fila (ex.: histórico de dados).
 */
public enum AuditSource {
    HTTP,
    SCHEDULED,
    ASYNC,
    JOB
}
