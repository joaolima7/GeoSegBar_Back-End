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

    /**
     * Devolve à fila os jobs que ficaram PAUSED após um erro recuperável, e
     * encerra de vez os que esgotaram as tentativas.
     *
     * Antes existiam DOIS caminhos para isso: este e
     * HistoricalDataJobService#recoverOrphanedJobs. Este aqui nunca funcionou —
     * findPausedJobs() era um esboço que devolvia lista vazia em qualquer
     * situação — e o outro empurrava o job para o Redis sem gravar QUEUED, de
     * modo que o consumidor descartava na sequência. O resultado foi o job 1
     * girar 4 dias em 19/08/2026 sem sair do lugar.
     *
     * Agora a responsabilidade é uma só: recoverOrphanedJobs faz a transição
     * PAUSED → QUEUED dentro de transação e só então enfileira. Aqui cuidamos
     * apenas do encerramento de quem já não tem retry disponível.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void failExhaustedPausedJobs() {
        try {
            List<HistoricalDataJobEntity> exhausted = jobService.findPausedJobsWithoutRetries();

            if (exhausted.isEmpty()) {
                return;
            }

            for (HistoricalDataJobEntity job : exhausted) {
                try {
                    jobService.markAsFailed(job.getId(),
                            "Job pausado após esgotar as 3 tentativas. Último erro: "
                            + job.getErrorMessage());
                    log.warn("Job {} marcado como FAILED (limite de tentativas atingido)", job.getId());
                } catch (Exception e) {
                    log.error("Erro ao encerrar job pausado {}: {}", job.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Erro ao encerrar jobs pausados sem retry: {}", e.getMessage(), e);
        }
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
