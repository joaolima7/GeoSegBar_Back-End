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

/**
 * Serviço para gerenciamento de jobs de coleta de dados históricos
 *
 * Responsável por: - Criar e enfileirar jobs - Gerenciar fila Redis (FIFO) -
 * Controlar transições de status - Atualizar progresso e checkpoints - Detectar
 * jobs travados
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalDataJobService {

    private static final String REDIS_QUEUE_KEY = "historical:data:queue";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long STALLED_JOB_TIMEOUT_HOURS = 1;

    private final HistoricalDataJobRepository jobRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Cria e enfileira um novo job de coleta de dados históricos
     *
     * Verifica se já existe job ativo para o instrumento antes de criar. O
     * período de coleta é: [hoje - 10 anos, hoje]
     *
     * @param instrumentId ID do instrumento
     * @param instrumentName Nome do instrumento
     * @return Job criado e enfileirado
     * @throws IllegalStateException se já existe job ativo para o instrumento
     */
    @Transactional
    public HistoricalDataJobEntity enqueueJob(Long instrumentId, String instrumentName) {
        // Verifica duplicatas
        if (hasActiveJobForInstrument(instrumentId)) {
            throw new IllegalStateException(
                    "Já existe um job ativo para o instrumento " + instrumentId
            );
        }

        // Calcula período de coleta: 10 anos
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(10);
        int totalMonths = (int) ChronoUnit.MONTHS.between(startDate, endDate);

        // Cria job
        HistoricalDataJobEntity job = new HistoricalDataJobEntity();
        job.setInstrumentId(instrumentId);
        job.setInstrumentName(instrumentName);
        job.setStatus(JobStatus.QUEUED);
        job.setStartDate(startDate);
        job.setEndDate(endDate);
        job.setCheckpointDate(startDate); // Inicia do começo
        job.setTotalMonths(totalMonths);
        job.setProcessedMonths(0);
        job.setCreatedReadings(0);
        job.setSkippedDays(0);
        job.setRetryCount(0);

        job = jobRepository.save(job);
        log.info("Job criado: id={}, instrumento={}, período={} a {}, totalMeses={}",
                job.getId(), instrumentName, startDate, endDate, totalMonths);

        // Adiciona à fila Redis
        pushToRedisQueue(job.getId());

        return job;
    }

    /**
     * Adiciona um job ID à fila Redis (RPUSH - adiciona no final)
     *
     * @param jobId ID do job
     */
    public void pushToRedisQueue(Long jobId) {
        redisTemplate.opsForList().rightPush(REDIS_QUEUE_KEY, jobId);
        log.debug("Job {} adicionado à fila Redis", jobId);
    }

    /**
     * Remove e retorna o próximo job ID da fila Redis (LPOP - remove do início)
     *
     * @return ID do próximo job, ou empty se fila vazia
     */
    public Optional<Long> popFromRedisQueue() {
        Object jobId = redisTemplate.opsForList().leftPop(REDIS_QUEUE_KEY);
        if (jobId instanceof Number) {
            log.debug("Job {} removido da fila Redis", jobId);
            return Optional.of(((Number) jobId).longValue());
        }
        return Optional.empty();
    }

    /**
     * Busca o próximo job QUEUED mais antigo do banco de dados
     *
     * Usado como fallback se a fila Redis estiver vazia.
     *
     * @return Job encontrado, ou empty
     */
    public Optional<HistoricalDataJobEntity> getNextQueuedJob() {
        List<HistoricalDataJobEntity> jobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);
        return jobs.isEmpty() ? Optional.empty() : Optional.of(jobs.get(0));
    }

    /**
     * Marca job como PROCESSING e registra timestamp de início
     *
     * @param jobId ID do job
     */
    @Transactional
    public void markAsProcessing(Long jobId) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        log.info("Job {} marcado como PROCESSING", jobId);
    }

    /**
     * Atualiza progresso do job (checkpoint, contadores)
     *
     * Chamado a cada batch de inserções (30-50 readings).
     *
     * @param jobId ID do job
     * @param checkpointDate Nova data de checkpoint
     * @param createdReadings Quantidade de readings criados neste batch
     * @param skippedDays Quantidade de dias pulados neste batch
     */
    @Transactional
    public void updateProgress(Long jobId, LocalDate checkpointDate, int createdReadings, int skippedDays) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        job.setCheckpointDate(checkpointDate);
        job.setCreatedReadings(job.getCreatedReadings() + createdReadings);
        job.setSkippedDays(job.getSkippedDays() + skippedDays);

        // Calcula meses processados
        int processedMonths = (int) ChronoUnit.MONTHS.between(job.getStartDate(), checkpointDate);
        job.setProcessedMonths(processedMonths);

        jobRepository.save(job);

        if (processedMonths % 12 == 0) { // Log a cada ano processado
            log.info("Job {} progresso: {}/{} meses ({:.1f}%), {} readings criados",
                    jobId, processedMonths, job.getTotalMonths(), job.getProgressPercentage(), job.getCreatedReadings());
        }
    }

    /**
     * Marca job como COMPLETED e registra timestamp de conclusão
     *
     * @param jobId ID do job
     */
    @Transactional
    public void markAsCompleted(Long jobId) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setProcessedMonths(job.getTotalMonths()); // Garante 100%
        jobRepository.save(job);

        long durationMinutes = ChronoUnit.MINUTES.between(job.getStartedAt(), job.getCompletedAt());
        log.info("Job {} COMPLETADO: {} readings criados, {} dias pulados, duração {} minutos",
                jobId, job.getCreatedReadings(), job.getSkippedDays(), durationMinutes);
    }

    /**
     * Marca job como FAILED e registra mensagem de erro
     *
     * @param jobId ID do job
     * @param errorMessage Mensagem de erro
     */
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

    /**
     * Marca job como PAUSED (erro recuperável, será retentado)
     *
     * @param jobId ID do job
     * @param errorMessage Mensagem de erro
     */
    @Transactional
    public void markAsPaused(Long jobId, String errorMessage) {
        HistoricalDataJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        job.setStatus(JobStatus.PAUSED);
        job.setErrorMessage(errorMessage);
        jobRepository.save(job);

        log.warn("Job {} PAUSADO: {} - Retry count: {}", jobId, errorMessage, job.getRetryCount());
    }

    /**
     * Incrementa contador de retry e verifica se atingiu limite
     *
     * @param jobId ID do job
     * @return true se ainda pode tentar novamente (retry < 3)
     */
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

    /**
     * Busca jobs travados em PROCESSING há mais de 1 hora
     *
     * @return Lista de jobs potencialmente travados
     */
    public List<HistoricalDataJobEntity> findStalledJobs() {
        LocalDateTime timeout = LocalDateTime.now().minusHours(STALLED_JOB_TIMEOUT_HOURS);
        return jobRepository.findStalledJobs(JobStatus.PROCESSING, timeout);
    }

    /**
     * Retorna contadores de jobs por status (para métricas Prometheus)
     *
     * @return Map com status e quantidade
     */
    public Map<JobStatus, Long> getJobCountsByStatus() {
        Map<JobStatus, Long> counts = new HashMap<>();
        for (JobStatus status : JobStatus.values()) {
            counts.put(status, jobRepository.countByStatus(status));
        }
        return counts;
    }

    /**
     * Verifica se existe job ativo (QUEUED ou PROCESSING) para o instrumento
     *
     * @param instrumentId ID do instrumento
     * @return true se existe job ativo
     */
    public boolean hasActiveJobForInstrument(Long instrumentId) {
        return jobRepository.existsActiveJobForInstrument(instrumentId);
    }

    /**
     * Busca job por ID
     *
     * @param jobId ID do job
     * @return Job encontrado
     */
    public Optional<HistoricalDataJobEntity> findById(Long jobId) {
        return jobRepository.findById(jobId);
    }

    /**
     * Busca histórico de jobs de um instrumento
     *
     * @param instrumentId ID do instrumento
     * @return Lista de jobs do instrumento
     */
    public List<HistoricalDataJobEntity> findJobsByInstrument(Long instrumentId) {
        return jobRepository.findByInstrumentIdOrderByCreatedAtDesc(instrumentId);
    }

    /**
     * Retorna tamanho atual da fila Redis
     *
     * @return Quantidade de jobs na fila
     */
    public Long getQueueSize() {
        Long size = redisTemplate.opsForList().size(REDIS_QUEUE_KEY);
        return size != null ? size : 0L;
    }
}
