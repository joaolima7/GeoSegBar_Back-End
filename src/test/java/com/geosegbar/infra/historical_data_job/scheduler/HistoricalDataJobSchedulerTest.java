package com.geosegbar.infra.historical_data_job.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.common.enums.JobStatus;
import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.infra.historical_data_job.processor.HistoricalDataJobProcessor;
import com.geosegbar.infra.historical_data_job.service.HistoricalDataJobService;

/**
 * Testes unitários para HistoricalDataJobScheduler
 */
@ExtendWith(MockitoExtension.class)
class HistoricalDataJobSchedulerTest {

    @Mock
    private HistoricalDataJobService jobService;

    @Mock
    private HistoricalDataJobProcessor jobProcessor;

    @InjectMocks
    private HistoricalDataJobScheduler scheduler;

    private HistoricalDataJobEntity queuedJob;
    private HistoricalDataJobEntity processingJob;
    private HistoricalDataJobEntity pausedJob;

    @BeforeEach
    void setUp() {
        // Job QUEUED
        queuedJob = new HistoricalDataJobEntity();
        queuedJob.setId(1L);
        queuedJob.setInstrumentId(100L);
        queuedJob.setInstrumentName("Régua Teste");
        queuedJob.setStatus(JobStatus.QUEUED);
        queuedJob.setStartDate(LocalDate.of(2016, 1, 1));
        queuedJob.setEndDate(LocalDate.now());
        queuedJob.setCheckpointDate(LocalDate.of(2016, 1, 1));
        queuedJob.setRetryCount(0);

        // Job PROCESSING (travado)
        processingJob = new HistoricalDataJobEntity();
        processingJob.setId(2L);
        processingJob.setInstrumentId(101L);
        processingJob.setInstrumentName("Régua Travada");
        processingJob.setStatus(JobStatus.PROCESSING);
        processingJob.setStartedAt(LocalDateTime.now().minusHours(2)); // 2h atrás
        processingJob.setRetryCount(1);

        // Job PAUSED
        pausedJob = new HistoricalDataJobEntity();
        pausedJob.setId(3L);
        pausedJob.setInstrumentId(102L);
        pausedJob.setInstrumentName("Régua Pausada");
        pausedJob.setStatus(JobStatus.PAUSED);
        pausedJob.setRetryCount(1);
    }

    @Test
    @DisplayName("Deve processar job da fila quando status é QUEUED")
    void shouldProcessJobFromQueue() {
        // Given
        when(jobService.getQueueSize()).thenReturn(1L);
        when(jobService.popFromRedisQueue()).thenReturn(Optional.of(1L), Optional.empty());
        when(jobService.findById(1L)).thenReturn(Optional.of(queuedJob));

        // When
        scheduler.processQueue();

        // Then
        verify(jobService).popFromRedisQueue();
        verify(jobService).findById(1L);
        verify(jobProcessor).processJob(1L);
    }

    @Test
    @DisplayName("Deve ignorar job que não está QUEUED")
    void shouldIgnoreJobNotQueued() {
        // Given
        processingJob.setId(1L);
        when(jobService.getQueueSize()).thenReturn(1L);
        when(jobService.popFromRedisQueue()).thenReturn(Optional.of(1L), Optional.empty());
        when(jobService.findById(1L)).thenReturn(Optional.of(processingJob));

        // When
        scheduler.processQueue();

        // Then
        verify(jobProcessor, never()).processJob(anyLong());
    }

    @Test
    @DisplayName("Deve fazer nada quando fila está vazia")
    void shouldDoNothingWhenQueueEmpty() {
        // Given
        when(jobService.getQueueSize()).thenReturn(0L);

        // When
        scheduler.processQueue();

        // Then
        verify(jobService, never()).popFromRedisQueue();
        verify(jobProcessor, never()).processJob(anyLong());
    }

    @Test
    @DisplayName("Deve processar múltiplos jobs até fila esvaziar")
    void shouldProcessMultipleJobsUntilQueueEmpty() {
        // Given
        HistoricalDataJobEntity job2 = new HistoricalDataJobEntity();
        job2.setId(2L);
        job2.setStatus(JobStatus.QUEUED);
        job2.setInstrumentName("Régua 2");

        when(jobService.getQueueSize()).thenReturn(2L);
        when(jobService.popFromRedisQueue())
                .thenReturn(Optional.of(1L))
                .thenReturn(Optional.of(2L))
                .thenReturn(Optional.empty());
        when(jobService.findById(1L)).thenReturn(Optional.of(queuedJob));
        when(jobService.findById(2L)).thenReturn(Optional.of(job2));

        // When
        scheduler.processQueue();

        // Then
        verify(jobProcessor).processJob(1L);
        verify(jobProcessor).processJob(2L);
    }

    @Test
    @DisplayName("Deve respeitar limite de 10 iterações por execução")
    void shouldRespectMaxIterationsLimit() {
        // Given
        when(jobService.getQueueSize()).thenReturn(15L);
        when(jobService.popFromRedisQueue()).thenReturn(Optional.of(1L));
        when(jobService.findById(1L)).thenReturn(Optional.of(queuedJob));

        // When
        scheduler.processQueue();

        // Then
        verify(jobProcessor, atMost(10)).processJob(anyLong());
    }

    @Test
    @DisplayName("Deve detectar e re-enfileirar jobs travados")
    void shouldDetectAndRequeueStalledJobs() {
        // Given
        when(jobService.findStalledJobs()).thenReturn(List.of(processingJob));
        when(jobService.incrementRetry(2L)).thenReturn(true); // Pode retry

        // When
        scheduler.detectStalledJobs();

        // Then
        verify(jobService).incrementRetry(2L);
        verify(jobService).markAsPaused(eq(2L), contains("travado"));
        verify(jobService).pushToRedisQueue(2L);
    }

    @Test
    @DisplayName("Deve falhar job travado após 3 tentativas")
    void shouldFailStalledJobAfterMaxRetries() {
        // Given
        processingJob.setRetryCount(2); // Já tentou 2 vezes
        when(jobService.findStalledJobs()).thenReturn(List.of(processingJob));
        when(jobService.incrementRetry(2L)).thenReturn(false); // Não pode mais retry

        // When
        scheduler.detectStalledJobs();

        // Then
        verify(jobService).incrementRetry(2L);
        verify(jobService).markAsFailed(eq(2L), contains("travado após 3 tentativas"));
        verify(jobService, never()).pushToRedisQueue(anyLong());
    }

    @Test
    @DisplayName("Deve fazer nada quando não há jobs travados")
    void shouldDoNothingWhenNoStalledJobs() {
        // Given
        when(jobService.findStalledJobs()).thenReturn(List.of());

        // When
        scheduler.detectStalledJobs();

        // Then
        verify(jobService, never()).incrementRetry(anyLong());
        verify(jobService, never()).markAsPaused(anyLong(), anyString());
    }

    @Test
    @DisplayName("Deve detectar múltiplos jobs travados")
    void shouldDetectMultipleStalledJobs() {
        // Given
        HistoricalDataJobEntity stalled2 = new HistoricalDataJobEntity();
        stalled2.setId(4L);
        stalled2.setStatus(JobStatus.PROCESSING);
        stalled2.setStartedAt(LocalDateTime.now().minusHours(3));
        stalled2.setRetryCount(0);
        stalled2.setInstrumentName("Régua 4");

        when(jobService.findStalledJobs()).thenReturn(Arrays.asList(processingJob, stalled2));
        when(jobService.incrementRetry(anyLong())).thenReturn(true);

        // When
        scheduler.detectStalledJobs();

        // Then
        verify(jobService, times(2)).incrementRetry(anyLong());
        verify(jobService, times(2)).markAsPaused(anyLong(), anyString());
        verify(jobService, times(2)).pushToRedisQueue(anyLong());
    }

    @Test
    @DisplayName("Deve exibir métricas em modo DEBUG")
    void shouldLogMetricsInDebugMode() {
        // Given
        when(jobService.getQueueSize()).thenReturn(5L);

        Map<JobStatus, Long> counts = new HashMap<>();
        counts.put(JobStatus.QUEUED, 3L);
        counts.put(JobStatus.PROCESSING, 2L);
        counts.put(JobStatus.PAUSED, 1L);
        counts.put(JobStatus.COMPLETED, 10L);
        counts.put(JobStatus.FAILED, 2L);

        when(jobService.getJobCountsByStatus()).thenReturn(counts);

        // When
        scheduler.logQueueMetrics();

        // Then
        verify(jobService).getQueueSize();
        verify(jobService).getJobCountsByStatus();
    }

    @Test
    @DisplayName("Não deve falhar se ocorrer exceção no processamento")
    void shouldNotFailOnException() {
        // Given
        when(jobService.getQueueSize()).thenReturn(1L);
        when(jobService.popFromRedisQueue()).thenThrow(new RuntimeException("Redis error"));

        // When & Then (não deve lançar exceção)
        scheduler.processQueue();

        verify(jobProcessor, never()).processJob(anyLong());
    }

    @Test
    @DisplayName("Deve continuar processando outros jobs se um falhar")
    void shouldContinueProcessingOtherJobsIfOneFails() {
        // Given
        HistoricalDataJobEntity job2 = new HistoricalDataJobEntity();
        job2.setId(2L);
        job2.setStatus(JobStatus.QUEUED);
        job2.setInstrumentName("Régua 2");

        when(jobService.getQueueSize()).thenReturn(2L);
        when(jobService.popFromRedisQueue())
                .thenReturn(Optional.of(1L))
                .thenReturn(Optional.of(2L))
                .thenReturn(Optional.empty());
        when(jobService.findById(1L)).thenThrow(new RuntimeException("DB error"));
        when(jobService.findById(2L)).thenReturn(Optional.of(job2));

        // When
        scheduler.processQueue();

        // Then
        verify(jobProcessor).processJob(2L); // Job 2 deve ser processado mesmo com erro no job 1
    }

    @Test
    @DisplayName("Deve ignorar job que não existe mais no banco")
    void shouldIgnoreJobNotFoundInDatabase() {
        // Given
        when(jobService.getQueueSize()).thenReturn(1L);
        when(jobService.popFromRedisQueue()).thenReturn(Optional.of(999L), Optional.empty());
        when(jobService.findById(999L)).thenReturn(Optional.empty());

        // When
        scheduler.processQueue();

        // Then
        verify(jobProcessor, never()).processJob(anyLong());
    }
}
