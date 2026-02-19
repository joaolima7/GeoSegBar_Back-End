package com.geosegbar.infra.historical_data_job.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.geosegbar.common.enums.JobStatus;
import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.infra.historical_data_job.processor.HistoricalDataJobProcessor;
import com.geosegbar.infra.historical_data_job.service.HistoricalDataJobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "historical-data-job.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class HistoricalDataJobScheduler {

    private final HistoricalDataJobService jobService;
    private final HistoricalDataJobProcessor jobProcessor;

    /**
     * Executado após o contexto Spring estar completamente inicializado.
     * Recupera jobs QUEUED/PAUSED que perderam sua entrada no Redis (ex: Redis
     * reiniciado sem persistência).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOrphanedJobsOnStartup() {
        try {
            int recovered = jobService.recoverOrphanedJobs();
            if (recovered > 0) {
                log.info("🔄 Startup recovery: {} job(s) re-enfileirado(s) no Redis", recovered);
            } else {
                log.debug("Startup recovery: nenhum job órfão encontrado");
            }
        } catch (Exception e) {
            log.error("Erro no recovery de jobs na inicialização: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 120000)
    public void processQueue() {
        try {

            Long queueSize = jobService.getQueueSize();
            if (queueSize == 0) {
                // Fila Redis vazia — verifica se há jobs no banco (proteção contra Redis restart)
                int recovered = jobService.recoverOrphanedJobs();
                if (recovered == 0) {
                    return;
                }
                log.info("🔄 Recovery: {} job(s) re-enfileirado(s) ao detectar fila Redis vazia", recovered);
            }

            log.debug("Fila de jobs históricos: {} jobs pendentes", queueSize);

            int processed = 0;
            int maxIterations = 10;

            while (processed < maxIterations) {
                Optional<Long> jobIdOpt = jobService.popFromRedisQueue();

                if (jobIdOpt.isEmpty()) {
                    break;
                }

                Long jobId = jobIdOpt.get();
                Optional<HistoricalDataJobEntity> jobOpt = jobService.findById(jobId);

                if (jobOpt.isEmpty()) {
                    log.warn("Job {} não encontrado no banco. Removendo da fila.", jobId);
                    continue;
                }

                HistoricalDataJobEntity job = jobOpt.get();

                if (job.getStatus() != JobStatus.QUEUED) {
                    log.warn("Job {} não está QUEUED (status: {}). Ignorando.",
                            jobId, job.getStatus());
                    continue;
                }

                log.info("📥 Disparando processamento do job {} (instrumento: {})",
                        jobId, job.getInstrumentName());

                jobProcessor.processJob(jobId);
                processed++;
            }

            if (processed > 0) {
                log.info("Scheduler processou {} job(s) da fila", processed);
            }

        } catch (Exception e) {
            log.error("Erro no scheduler de processamento da fila: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void detectStalledJobs() {
        try {
            List<HistoricalDataJobEntity> stalledJobs = jobService.findStalledJobs();

            if (stalledJobs.isEmpty()) {
                return;
            }

            log.warn("⚠️ Detectados {} job(s) travado(s) em PROCESSING", stalledJobs.size());

            for (HistoricalDataJobEntity job : stalledJobs) {
                try {
                    LocalDateTime stuckSince = job.getStartedAt();
                    long minutesStuck = java.time.Duration.between(stuckSince, LocalDateTime.now()).toMinutes();

                    log.warn("Job {} travado há {} minutos (instrumento: {}, retry: {}/3)",
                            job.getId(), minutesStuck, job.getInstrumentName(), job.getRetryCount());

                    boolean canRetry = jobService.incrementRetry(job.getId());

                    if (canRetry) {

                        jobService.markAsPaused(job.getId(),
                                "Job travado por " + minutesStuck + " minutos - re-enfileirando");
                        jobService.pushToRedisQueue(job.getId());

                        log.info("Job {} pausado e re-enfileirado para retry {}/3",
                                job.getId(), job.getRetryCount() + 1);
                    } else {

                        jobService.markAsFailed(job.getId(),
                                "Job travado após 3 tentativas (" + minutesStuck + " minutos)");

                        log.error("Job {} marcado como FAILED após 3 tentativas", job.getId());
                    }

                } catch (Exception e) {
                    log.error("Erro ao processar job travado {}: {}", job.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Erro ao detectar jobs travados: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void requeuePausedJobs() {
        try {
            List<HistoricalDataJobEntity> pausedJobs
                    = jobService.getJobCountsByStatus().get(JobStatus.PAUSED) > 0
                    ? findPausedJobs()
                    : List.of();

            if (pausedJobs.isEmpty()) {
                return;
            }

            log.info("🔄 Re-enfileirando {} job(s) pausado(s)", pausedJobs.size());

            for (HistoricalDataJobEntity job : pausedJobs) {
                try {

                    if (job.getRetryCount() >= 3) {
                        jobService.markAsFailed(job.getId(),
                                "Job pausado com retry count >= 3");
                        log.warn("Job {} marcado como FAILED (retry limit reached)", job.getId());
                        continue;
                    }

                    job.setStatus(JobStatus.QUEUED);
                    jobService.pushToRedisQueue(job.getId());

                    log.info("Job {} re-enfileirado (retry {}/3, instrumento: {})",
                            job.getId(), job.getRetryCount(), job.getInstrumentName());

                } catch (Exception e) {
                    log.error("Erro ao re-enfileirar job pausado {}: {}",
                            job.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Erro ao re-enfileirar jobs pausados: {}", e.getMessage(), e);
        }
    }

    private List<HistoricalDataJobEntity> findPausedJobs() {

        return jobService.findById(0L).map(job -> List.<HistoricalDataJobEntity>of())
                .orElseGet(() -> {

                    return List.of();
                });
    }

    @Scheduled(cron = "0 */2 * * * *")
    public void logQueueMetrics() {
        if (!log.isDebugEnabled()) {
            return;
        }

        try {
            Long queueSize = jobService.getQueueSize();
            var statusCounts = jobService.getJobCountsByStatus();

            log.debug("📊 Métricas de jobs históricos:");
            log.debug("  - Fila Redis: {} jobs", queueSize);
            log.debug("  - QUEUED: {} | PROCESSING: {} | PAUSED: {}",
                    statusCounts.getOrDefault(JobStatus.QUEUED, 0L),
                    statusCounts.getOrDefault(JobStatus.PROCESSING, 0L),
                    statusCounts.getOrDefault(JobStatus.PAUSED, 0L));
            log.debug("  - COMPLETED: {} | FAILED: {}",
                    statusCounts.getOrDefault(JobStatus.COMPLETED, 0L),
                    statusCounts.getOrDefault(JobStatus.FAILED, 0L));

        } catch (Exception e) {
            log.error("Erro ao exibir métricas: {}", e.getMessage());
        }
    }
}
