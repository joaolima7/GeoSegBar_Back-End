package com.geosegbar.infra.historical_data_job.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.geosegbar.common.enums.JobStatus;
import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.infra.historical_data_job.persistence.HistoricalDataJobRepository;

/**
 * Testes unitários para HistoricalDataJobService
 */
@ExtendWith(MockitoExtension.class)
class HistoricalDataJobServiceTest {

    @Mock
    private HistoricalDataJobRepository jobRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    @InjectMocks
    private HistoricalDataJobService jobService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    @DisplayName("Deve criar e enfileirar um job com sucesso")
    void shouldCreateAndEnqueueJob() {
        // Given
        Long instrumentId = 100L;
        String instrumentName = "Régua Linimétrica XYZ";

        HistoricalDataJobEntity savedJob = new HistoricalDataJobEntity();
        savedJob.setId(1L);
        savedJob.setInstrumentId(instrumentId);
        savedJob.setInstrumentName(instrumentName);
        savedJob.setStatus(JobStatus.QUEUED);
        savedJob.setStartDate(LocalDate.now().minusYears(10));
        savedJob.setEndDate(LocalDate.now());
        savedJob.setTotalMonths(120);

        when(jobRepository.existsActiveJobForInstrument(instrumentId)).thenReturn(false);
        when(jobRepository.save(any(HistoricalDataJobEntity.class))).thenReturn(savedJob);
        when(listOperations.rightPush(anyString(), any())).thenReturn(1L);

        // When
        HistoricalDataJobEntity result = jobService.enqueueJob(instrumentId, instrumentName);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(instrumentId, result.getInstrumentId());
        assertEquals(instrumentName, result.getInstrumentName());
        assertEquals(JobStatus.QUEUED, result.getStatus());

        verify(jobRepository).existsActiveJobForInstrument(instrumentId);
        verify(jobRepository).save(any(HistoricalDataJobEntity.class));
        verify(listOperations).rightPush(eq("historical:data:queue"), eq(1L));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar job duplicado")
    void shouldThrowExceptionWhenCreatingDuplicateJob() {
        // Given
        Long instrumentId = 100L;
        when(jobRepository.existsActiveJobForInstrument(instrumentId)).thenReturn(true);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> jobService.enqueueJob(instrumentId, "Test")
        );

        assertTrue(exception.getMessage().contains("Já existe um job ativo"));
        verify(jobRepository, never()).save(any());
        verify(listOperations, never()).rightPush(anyString(), any());
    }

    @Test
    @DisplayName("Deve remover job da fila Redis (pop)")
    void shouldPopFromRedisQueue() {
        // Given
        Long jobId = 42L;
        when(listOperations.leftPop(anyString())).thenReturn(jobId);

        // When
        Optional<Long> result = jobService.popFromRedisQueue();

        // Then
        assertTrue(result.isPresent());
        assertEquals(jobId, result.get());
        verify(listOperations).leftPop("historical:data:queue");
    }

    @Test
    @DisplayName("Deve retornar empty quando fila Redis está vazia")
    void shouldReturnEmptyWhenQueueIsEmpty() {
        // Given
        when(listOperations.leftPop(anyString())).thenReturn(null);

        // When
        Optional<Long> result = jobService.popFromRedisQueue();

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve marcar job como PROCESSING")
    void shouldMarkJobAsProcessing() {
        // Given
        Long jobId = 1L;
        HistoricalDataJobEntity job = new HistoricalDataJobEntity();
        job.setId(jobId);
        job.setStatus(JobStatus.QUEUED);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        // When
        jobService.markAsProcessing(jobId);

        // Then
        assertEquals(JobStatus.PROCESSING, job.getStatus());
        assertNotNull(job.getStartedAt());
        verify(jobRepository).save(job);
    }

    @Test
    @DisplayName("Deve incrementar retry e retornar se pode tentar novamente")
    void shouldIncrementRetryAndCheckIfCanRetry() {
        // Given
        Long jobId = 1L;
        HistoricalDataJobEntity job = new HistoricalDataJobEntity();
        job.setId(jobId);
        job.setRetryCount(1);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        // When
        boolean canRetry = jobService.incrementRetry(jobId);

        // Then
        assertTrue(canRetry); // 2 < 3
        assertEquals(2, job.getRetryCount());
        verify(jobRepository).save(job);
    }

    @Test
    @DisplayName("Deve retornar false quando atingir limite de retry")
    void shouldReturnFalseWhenRetryLimitReached() {
        // Given
        Long jobId = 1L;
        HistoricalDataJobEntity job = new HistoricalDataJobEntity();
        job.setId(jobId);
        job.setRetryCount(2); // Último retry

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        // When
        boolean canRetry = jobService.incrementRetry(jobId);

        // Then
        assertFalse(canRetry); // 3 >= 3
        assertEquals(3, job.getRetryCount());
    }

    @Test
    @DisplayName("Deve atualizar progresso do job")
    void shouldUpdateJobProgress() {
        // Given
        Long jobId = 1L;
        LocalDate checkpoint = LocalDate.of(2020, 6, 15);
        HistoricalDataJobEntity job = new HistoricalDataJobEntity();
        job.setId(jobId);
        job.setStartDate(LocalDate.of(2016, 1, 1));
        job.setCreatedReadings(100);
        job.setSkippedDays(5);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        // When
        jobService.updateProgress(jobId, checkpoint, 30, 2);

        // Then
        assertEquals(checkpoint, job.getCheckpointDate());
        assertEquals(130, job.getCreatedReadings()); // 100 + 30
        assertEquals(7, job.getSkippedDays()); // 5 + 2
        assertTrue(job.getProcessedMonths() > 0);
        verify(jobRepository).save(job);
    }

    @Test
    @DisplayName("Deve verificar se existe job ativo para instrumento")
    void shouldCheckIfActiveJobExists() {
        // Given
        Long instrumentId = 100L;
        when(jobRepository.existsActiveJobForInstrument(instrumentId)).thenReturn(true);

        // When
        boolean hasActive = jobService.hasActiveJobForInstrument(instrumentId);

        // Then
        assertTrue(hasActive);
        verify(jobRepository).existsActiveJobForInstrument(instrumentId);
    }
}
