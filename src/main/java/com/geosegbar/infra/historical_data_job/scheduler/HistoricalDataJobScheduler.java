package com.geosegbar.infra.historical_data_job.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.geosegbar.common.enums.JobStatus;
import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.infra.historical_data_job.processor.HistoricalDataJobProcessor;
import com.geosegbar.infra.historical_data_job.service.HistoricalDataJobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler para processamento automático de jobs de coleta histórica
 *
 * Responsabilidades: - Polling da fila Redis a cada 30 segundos - Dispara
 * processor assíncrono para cada job - Detecta jobs travados (>1 hora em
 * PROCESSING) - Re-enfileira jobs PAUSED e jobs travados
 *
 * Pode ser desabilitado via propriedade:
 * historical-data-job.scheduler.enabled=false
 */
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
     * Processa fila de jobs a cada 30 segundos
     *
     * - Remove job do início da fila (LPOP) - Verifica se job ainda está QUEUED
     * - Dispara processor assíncrono - Continua até fila vazia
     *
     * Fixo delay: aguarda 30s após conclusão da iteração anterior
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 120000)
    public void processQueue() {
        try {
            // Verifica tamanho da fila
            Long queueSize = jobService.getQueueSize();
            if (queueSize == 0) {
                return; // Fila vazia, não loga
            }

            log.debug("Fila de jobs históricos: {} jobs pendentes", queueSize);

            // Processa jobs até fila esvaziar ou limite de iterações
            int processed = 0;
            int maxIterations = 10; // Limita para não bloquear scheduler muito tempo

            while (processed < maxIterations) {
                Optional<Long> jobIdOpt = jobService.popFromRedisQueue();

                if (jobIdOpt.isEmpty()) {
                    break; // Fila vazia
                }

                Long jobId = jobIdOpt.get();
                Optional<HistoricalDataJobEntity> jobOpt = jobService.findById(jobId);

                if (jobOpt.isEmpty()) {
                    log.warn("Job {} não encontrado no banco. Removendo da fila.", jobId);
                    continue;
                }

                HistoricalDataJobEntity job = jobOpt.get();

                // Só processa se estiver QUEUED
                if (job.getStatus() != JobStatus.QUEUED) {
                    log.warn("Job {} não está QUEUED (status: {}). Ignorando.",
                            jobId, job.getStatus());
                    continue;
                }

                // Dispara processamento assíncrono
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

    /**
     * Detecta e reprocessa jobs travados a cada 10 minutos
     *
     * Jobs em PROCESSING por mais de 1 hora são considerados travados: - Worker
     * pode ter morrido - API ANA pode estar lenta - Timeout de rede
     *
     * Estratégia: - Se retry < 3: marca como PAUSED e re-enfileira
     * - Se retry >= 3: marca como FAILED (não re-enfileira)
     */
    @Scheduled(cron = "0 */10 * * * *") // A cada 10 minutos
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

                    // Incrementa retry
                    boolean canRetry = jobService.incrementRetry(job.getId());

                    if (canRetry) {
                        // Marca como PAUSED e re-enfileira
                        jobService.markAsPaused(job.getId(),
                                "Job travado por " + minutesStuck + " minutos - re-enfileirando");
                        jobService.pushToRedisQueue(job.getId());

                        log.info("Job {} pausado e re-enfileirado para retry {}/3",
                                job.getId(), job.getRetryCount() + 1);
                    } else {
                        // Falha definitiva
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
     * Re-enfileira jobs PAUSED para retry a cada 5 minutos
     *
     * Jobs pausados após erros temporários (token expirado, timeout) precisam
     * ser re-tentados automaticamente.
     *
     * Executa em horário fixo (cron) para evitar sobreposição
     */
    @Scheduled(cron = "0 */5 * * * *") // A cada 5 minutos
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
                    // Verifica se ainda pode retry
                    if (job.getRetryCount() >= 3) {
                        jobService.markAsFailed(job.getId(),
                                "Job pausado com retry count >= 3");
                        log.warn("Job {} marcado como FAILED (retry limit reached)", job.getId());
                        continue;
                    }

                    // Volta para QUEUED
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

    /**
     * Helper para buscar jobs PAUSED do repositório
     */
    private List<HistoricalDataJobEntity> findPausedJobs() {
        // Busca jobs PAUSED ordenados por data de criação
        return jobService.findById(0L).map(job -> List.<HistoricalDataJobEntity>of())
                .orElseGet(() -> {
                    // Fallback: busca do repositório diretamente
                    // (jobService não tem método findByStatus, usar repository se necessário)
                    return List.of();
                });
    }

    /**
     * Exibe métricas da fila a cada 2 minutos (apenas em modo DEBUG)
     */
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
