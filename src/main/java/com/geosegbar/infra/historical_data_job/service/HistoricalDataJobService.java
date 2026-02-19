package com.geosegbar.infra.historical_data_job.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.JobStatus;
import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.infra.historical_data_job.persistence.HistoricalDataJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalDataJobService {

    private static final String REDIS_QUEUE_KEY = "historical:data:queue";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long STALLED_JOB_TIMEOUT_HOURS = 1;

    private final HistoricalDataJobRepository jobRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public HistoricalDataJobEntity enqueueJob(Long instrumentId, String instrumentName) {

        if (hasActiveJobForInstrument(instrumentId)) {
            throw new IllegalStateException(
                    "Já existe um job ativo para o instrumento " + instrumentId
            );
        }

        LocalDate startDate = LocalDate.of(2015, 1, 1);
        LocalDate endDate = LocalDate.now().plusMonths(1);
        int totalMonths = (int) ChronoUnit.MONTHS.between(startDate, endDate);

        HistoricalDataJobEntity job = new HistoricalDataJobEntity();
        job.setInstrumentId(instrumentId);
        job.setInstrumentName(instrumentName);
        job.setStatus(JobStatus.QUEUED);
        job.setStartDate(startDate);
        job.setEndDate(endDate);
        job.setCheckpointDate(startDate);
        job.setTotalMonths(totalMonths);
        job.setProcessedMonths(0);
        job.setCreatedReadings(0);
        job.setSkippedDays(0);
        job.setRetryCount(0);

        job = jobRepository.save(job);
        log.info("Job criado: id={}, instrumento={}, período={} a {}, totalMeses={}",
                job.getId(), instrumentName, startDate, endDate, totalMonths);

        pushToRedisQueue(job.getId());

        return job;
    }

    public void pushToRedisQueue(Long jobId) {
        redisTemplate.opsForList().rightPush(REDIS_QUEUE_KEY, jobId);
        log.debug("Job {} adicionado à fila Redis", jobId);
    }

    public Optional<Long> popFromRedisQueue() {
        Object jobId = redisTemplate.opsForList().leftPop(REDIS_QUEUE_KEY);
        if (jobId instanceof Number) {
            log.debug("Job {} removido da fila Redis", jobId);
            return Optional.of(((Number) jobId).longValue());
        }
        return Optional.empty();
    }

    // ✅ OTIMIZADO: Usa findTopBy para pegar apenas 1 registro do banco
    @Transactional(readOnly = true)
    public Optional<HistoricalDataJobEntity> getNextQueuedJob() {
        return jobRepository.findTopByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);
    }

    @Transactional
    public void markAsProcessing(Long jobId) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        log.info("Job {} marcado como PROCESSING", jobId);
    }

    @Transactional
    public void updateProgress(Long jobId, LocalDate checkpointDate, int createdReadings, int skippedDays) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        job.setCheckpointDate(checkpointDate);
        job.setCreatedReadings(job.getCreatedReadings() + createdReadings);
        job.setSkippedDays(job.getSkippedDays() + skippedDays);

        int processedMonths = (int) ChronoUnit.MONTHS.between(job.getStartDate(), checkpointDate);
        job.setProcessedMonths(processedMonths);

        jobRepository.save(job);

        if (processedMonths % 12 == 0) {
            log.info("Job {} progresso: {}/{} meses ({:.1f}%), {} readings criados",
                    jobId, processedMonths, job.getTotalMonths(), job.getProgressPercentage(), job.getCreatedReadings());
        }
    }

    @Transactional
    public void markAsCompleted(Long jobId) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setProcessedMonths(job.getTotalMonths());
        jobRepository.save(job);

        long durationMinutes = ChronoUnit.MINUTES.between(job.getStartedAt(), job.getCompletedAt());
        log.info("Job {} COMPLETADO: {} readings criados, {} dias pulados, duração {} minutos",
                jobId, job.getCreatedReadings(), job.getSkippedDays(), durationMinutes);
    }

    @Transactional
    public void markAsFailed(Long jobId, String errorMessage) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        job.setErrorMessage(errorMessage);
        jobRepository.save(job);

        log.error("Job {} FALHOU: {} - Progresso: {:.1f}%",
                jobId, errorMessage, job.getProgressPercentage());
    }

    @Transactional
    public void markAsPaused(Long jobId, String errorMessage) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        job.setStatus(JobStatus.PAUSED);
        job.setErrorMessage(errorMessage);
        jobRepository.save(job);

        log.warn("Job {} PAUSADO: {} - Retry count: {}", jobId, errorMessage, job.getRetryCount());
    }

    @Transactional
    public boolean incrementRetry(Long jobId) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        int newRetryCount = job.getRetryCount() + 1;
        job.setRetryCount(newRetryCount);
        jobRepository.save(job);

        boolean canRetry = newRetryCount < MAX_RETRY_ATTEMPTS;
        log.info("Job {} retry incrementado: {}/{} - Pode tentar novamente: {}",
                jobId, newRetryCount, MAX_RETRY_ATTEMPTS, canRetry);

        return canRetry;
    }

    @Transactional(readOnly = true)
    public List<HistoricalDataJobEntity> findStalledJobs() {
        LocalDateTime timeout = LocalDateTime.now().minusHours(STALLED_JOB_TIMEOUT_HOURS);
        return jobRepository.findStalledJobs(JobStatus.PROCESSING, timeout);
    }

    // ✅ OTIMIZADO: Usa GROUP BY no banco para evitar múltiplas queries (N+1)
    @Transactional(readOnly = true)
    public Map<JobStatus, Long> getJobCountsByStatus() {
        List<Object[]> results = jobRepository.countJobsGroupedByStatus();
        Map<JobStatus, Long> counts = new HashMap<>();

        // Inicializa com zero para todos os status
        for (JobStatus status : JobStatus.values()) {
            counts.put(status, 0L);
        }

        // Preenche com os valores do banco
        for (Object[] result : results) {
            JobStatus status = (JobStatus) result[0];
            Long count = (Long) result[1];
            counts.put(status, count);
        }

        return counts;
    }

    @Transactional(readOnly = true)
    public boolean hasActiveJobForInstrument(Long instrumentId) {
        return jobRepository.existsActiveJobForInstrument(instrumentId);
    }

    @Transactional(readOnly = true)
    public Optional<HistoricalDataJobEntity> findById(Long jobId) {
        return jobRepository.findById(jobId);
    }

    @Transactional(readOnly = true)
    public List<HistoricalDataJobEntity> findJobsByInstrument(Long instrumentId) {
        return jobRepository.findByInstrumentIdOrderByCreatedAtDesc(instrumentId);
    }

    public Long getQueueSize() {
        Long size = redisTemplate.opsForList().size(REDIS_QUEUE_KEY);
        return size != null ? size : 0L;
    }

    /**
     * Recupera jobs QUEUED/PAUSED do banco que perderam sua entrada na fila
     * Redis. Chamado no startup e quando a fila Redis está vazia como proteção
     * contra reinicializações do Redis (sem persistência).
     *
     * @return número de jobs re-enfileirados
     */
    @Transactional(readOnly = true)
    public int recoverOrphanedJobs() {
        List<HistoricalDataJobEntity> queuedJobs
                = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);
        List<HistoricalDataJobEntity> pausedJobs
                = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.PAUSED);

        int recovered = 0;

        for (HistoricalDataJobEntity job : queuedJobs) {
            pushToRedisQueue(job.getId());
            recovered++;
            log.info("🔄 Job {} (QUEUED) re-enfileirado na fila Redis (recovery)", job.getId());
        }

        // Paused jobs com retry disponível também voltam para fila
        for (HistoricalDataJobEntity job : pausedJobs) {
            if (job.getRetryCount() < MAX_RETRY_ATTEMPTS) {
                pushToRedisQueue(job.getId());
                recovered++;
                log.info("🔄 Job {} (PAUSED, retry {}/{}) re-enfileirado na fila Redis (recovery)",
                        job.getId(), job.getRetryCount(), MAX_RETRY_ATTEMPTS);
            }
        }

        return recovered;
    }
}
