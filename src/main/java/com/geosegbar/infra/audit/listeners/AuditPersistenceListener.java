package com.geosegbar.infra.audit.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.geosegbar.infra.audit.events.AuditEvent;
import com.geosegbar.infra.audit.persistence.jpa.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Persiste registros de auditoria de forma assíncrona, em um pool de threads
 * dedicado ({@code auditExecutor}). A auditoria é best-effort: qualquer falha ao
 * gravar é apenas logada e NUNCA propagada — não pode afetar a requisição/job de
 * origem.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditPersistenceListener {

    private final AuditLogRepository auditLogRepository;

    @Async("auditExecutor")
    @EventListener
    public void onAuditEvent(AuditEvent event) {
        String action = event.getAuditLog() != null ? event.getAuditLog().getAction() : "null";
        try {
            log.debug("[AUDIT] ... gravando no banco (action={}) na thread {}",
                    action, Thread.currentThread().getName());
            var saved = auditLogRepository.save(event.getAuditLog());
            log.debug("[AUDIT] OK gravado: id={} action={} status={}",
                    saved.getId(), saved.getAction(), saved.getStatus());
        } catch (Exception e) {
            log.error("[AUDIT] FALHA ao persistir registro de auditoria (action={}): {}",
                    action, e.getMessage(), e);
        }
    }
}
