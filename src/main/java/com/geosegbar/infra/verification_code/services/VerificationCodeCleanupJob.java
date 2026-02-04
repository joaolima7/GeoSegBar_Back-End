package com.geosegbar.infra.verification_code.services;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.infra.verification_code.persistence.jpa.VerificationCodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeCleanupJob {

    private final VerificationCodeRepository verificationCodeRepository;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void purgeUsedOrExpiredCodes() {
        try {
            LocalDateTime now = LocalDateTime.now();

            int deleted = verificationCodeRepository.deleteAllUsedOrExpired(now);
            log.info("VerificationCodeCleanupJob: removed {} codes (used or expired) at {}", deleted, now);
        } catch (Exception ex) {
            log.error("VerificationCodeCleanupJob failed", ex);
        }
    }
}
