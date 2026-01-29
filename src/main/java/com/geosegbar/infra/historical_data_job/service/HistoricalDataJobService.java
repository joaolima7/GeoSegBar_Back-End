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

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(10);
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

    public Optional<HistoricalDataJobEntity> getNextQueuedJob() {
        List<HistoricalDataJobEntity> jobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);
        return jobs.isEmpty() ? Optional.empty() : Optional.of(jobs.get(0));
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

    public List<HistoricalDataJobEntity> findStalledJobs() {
        LocalDateTime timeout = LocalDateTime.now().minusHours(STALLED_JOB_TIMEOUT_HOURS);
        return jobRepository.findStalledJobs(JobStatus.PROCESSING, timeout);
    }

    public Map<JobStatus, Long> getJobCountsByStatus() {
        Map<JobStatus, Long> counts = new HashMap<>();
        for (JobStatus status : JobStatus.values()) {
            counts.put(status, jobRepository.countByStatus(status));
        }
        return counts;
    }

    public boolean hasActiveJobForInstrument(Long instrumentId) {
        return jobRepository.existsActiveJobForInstrument(instrumentId);
    }

    public Optional<HistoricalDataJobEntity> findById(Long jobId) {
        return jobRepository.findById(jobId);
    }

    public List<HistoricalDataJobEntity> findJobsByInstrument(Long instrumentId) {
        return jobRepository.findByInstrumentIdOrderByCreatedAtDesc(instrumentId);
    }

    public Long getQueueSize() {
        Long size = redisTemplate.opsForList().size(REDIS_QUEUE_KEY);
        return size != null ? size : 0L;
    }
}
