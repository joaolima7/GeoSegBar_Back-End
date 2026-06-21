package com.geosegbar.infra.verification_code.services;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.infra.audit.services.AuditService;
import com.geosegbar.infra.verification_code.persistence.jpa.VerificationCodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeCleanupJob {

    private static final String ACTION = "JOB_VERIFICATION_CODE_CLEANUP";
    private static final String ACTION_LABEL = "Limpeza de códigos de verificação";

    private final VerificationCodeRepository verificationCodeRepository;
    private final AuditService auditService;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void purgeUsedOrExpiredCodes() {
        long start = System.nanoTime();
        String traceId = auditService.newTraceId();
        try {
            LocalDateTime now = LocalDateTime.now();

            int deleted = verificationCodeRepository.deleteAllUsedOrExpired(now);
            log.info("VerificationCodeCleanupJob: removed {} codes (used or expired) at {}", deleted, now);

            auditService.recordJobSuccess(ACTION, ACTION_LABEL, AuditSource.SCHEDULED,
                    "Removidos " + deleted + " código(s) usados ou expirados.",
                    traceId, durationMs(start));
        } catch (Exception ex) {
            log.error("VerificationCodeCleanupJob failed", ex);
            auditService.recordJobError(ACTION, ACTION_LABEL, AuditSource.SCHEDULED,
                    "Falha na limpeza de códigos de verificação.", ex, traceId, durationMs(start));
        }
    }

    private long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
