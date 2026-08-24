package com.geosegbar.infra.password_setup.services;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.infra.audit.services.AuditService;
import com.geosegbar.infra.password_setup.persistence.jpa.PasswordSetupTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordSetupTokenCleanupJob {

    private static final String ACTION = "JOB_PASSWORD_SETUP_TOKEN_CLEANUP";
    private static final String ACTION_LABEL = "Limpeza de tokens de definição de senha";

    private final PasswordSetupTokenRepository passwordSetupTokenRepository;
    private final AuditService auditService;

    @Scheduled(cron = "0 10 1 * * *")
    @Transactional
    public void purgeUsedOrExpiredTokens() {
        long start = System.nanoTime();
        String traceId = auditService.newTraceId();
        try {
            LocalDateTime now = LocalDateTime.now();

            int deleted = passwordSetupTokenRepository.deleteAllUsedOrExpired(now);
            log.info("PasswordSetupTokenCleanupJob: removed {} tokens (used or expired) at {}", deleted, now);

            auditService.recordJobSuccess(ACTION, ACTION_LABEL, AuditSource.SCHEDULED,
                    "Removidos " + deleted + " token(s) de definição de senha usados ou expirados.",
                    traceId, durationMs(start));
        } catch (Exception ex) {
            log.error("PasswordSetupTokenCleanupJob failed", ex);
            auditService.recordJobError(ACTION, ACTION_LABEL, AuditSource.SCHEDULED,
                    "Falha na limpeza de tokens de definição de senha.", ex, traceId, durationMs(start));
        }
    }

    private long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
