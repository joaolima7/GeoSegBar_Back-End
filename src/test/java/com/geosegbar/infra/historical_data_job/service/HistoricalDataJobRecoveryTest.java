package com.geosegbar.infra.historical_data_job.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.geosegbar.common.enums.JobStatus;
import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.infra.historical_data_job.persistence.HistoricalDataJobRepository;

/**
 * Regressão do incidente de 19/08/2026, em que o job 1 girou 4 dias sem sair do
 * lugar.
 *
 * Duas causas, ambas cobertas aqui:
 *
 * 1. recoverOrphanedJobs empurrava o job PAUSED para o Redis sem gravar QUEUED.
 * O consumidor recusa o que não está QUEUED, então o job era enfileirado e
 * descartado a cada 30s, indefinidamente.
 *
 * 2. A API da ANA respondeu HTML; a mensagem inteira foi para error_message
 * (varchar(2000)) e o UPDATE estourou, deixando o job preso em PROCESSING.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Regressão - recuperação de jobs históricos (incidente 19/08/2026)")
class HistoricalDataJobRecoveryTest {

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
        lenient().when(jobRepository.save(any(HistoricalDataJobEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------- livelock
    @Test
    @DisplayName("Job PAUSED com retry disponível vira QUEUED no banco antes de ir para a fila")
    void pausedJobBecomesQueuedBeforeEnqueue() {
        HistoricalDataJobEntity paused = job(1L, JobStatus.PAUSED, 2);

        when(jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.QUEUED)).thenReturn(List.of());
        when(jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.PAUSED)).thenReturn(List.of(paused));

        int recuperados = jobService.recoverOrphanedJobs();

        assertThat(recuperados).isEqualTo(1);

        // O ponto do bug: sem esta gravação o consumidor descarta o job para sempre.
        ArgumentCaptor<HistoricalDataJobEntity> salvo = ArgumentCaptor.forClass(HistoricalDataJobEntity.class);
        verify(jobRepository).save(salvo.capture());
        assertThat(salvo.getValue().getStatus()).isEqualTo(JobStatus.QUEUED);

        verify(listOperations).rightPush(any(), any());
    }

    @Test
    @DisplayName("Job PAUSED sem retry não volta para a fila nem é regravado")
    void exhaustedPausedJobIsNotRequeued() {
        HistoricalDataJobEntity exausto = job(2L, JobStatus.PAUSED, 3);

        when(jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.QUEUED)).thenReturn(List.of());
        when(jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.PAUSED)).thenReturn(List.of(exausto));

        assertThat(jobService.recoverOrphanedJobs()).isZero();

        verify(jobRepository, never()).save(any());
        verify(listOperations, never()).rightPush(any(), any());
        assertThat(exausto.getStatus()).isEqualTo(JobStatus.PAUSED);
    }

    @Test
    @DisplayName("Job já QUEUED é reenfileirado sem regravação desnecessária")
    void queuedJobIsJustPushed() {
        when(jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.QUEUED))
                .thenReturn(List.of(job(3L, JobStatus.QUEUED, 0)));
        when(jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.PAUSED)).thenReturn(List.of());

        assertThat(jobService.recoverOrphanedJobs()).isEqualTo(1);

        verify(listOperations).rightPush(any(), any());
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Jobs esgotados são listados para encerramento como FAILED")
    void listsExhaustedPausedJobs() {
        when(jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.PAUSED))
                .thenReturn(List.of(job(1L, JobStatus.PAUSED, 1), job(2L, JobStatus.PAUSED, 3)));

        List<HistoricalDataJobEntity> esgotados = jobService.findPausedJobsWithoutRetries();

        assertThat(esgotados).extracting(HistoricalDataJobEntity::getId).containsExactly(2L);
    }

    // ---------------------------------------------------- truncamento do erro
    @Test
    @DisplayName("Mensagem de erro gigante não impede o registro da pausa")
    void hugeErrorMessageDoesNotBreakPause() {
        HistoricalDataJobEntity paused = job(1L, JobStatus.PROCESSING, 0);
        when(jobRepository.findById(1L)).thenReturn(java.util.Optional.of(paused));

        // Foi exatamente isto que chegou da ANA em 19/08: uma página HTML inteira.
        String htmlGigante = "<html><body>" + "erro ".repeat(2000) + "</body></html>";

        jobService.markAsPaused(1L, htmlGigante);

        assertThat(paused.getStatus()).isEqualTo(JobStatus.PAUSED);
        assertThat(paused.getErrorMessage()).hasSizeLessThanOrEqualTo(2000);
        assertThat(paused.getErrorMessage()).endsWith("...");
    }

    @Test
    @DisplayName("Mensagem gigante também não impede o registro da falha")
    void hugeErrorMessageDoesNotBreakFailure() {
        HistoricalDataJobEntity falho = job(1L, JobStatus.PROCESSING, 3);
        when(jobRepository.findById(1L)).thenReturn(java.util.Optional.of(falho));

        jobService.markAsFailed(1L, "x".repeat(50_000));

        assertThat(falho.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(falho.getErrorMessage()).hasSizeLessThanOrEqualTo(2000);
    }

    @Test
    @DisplayName("Mensagem curta é preservada integralmente")
    void shortErrorMessageIsKept() {
        HistoricalDataJobEntity paused = job(1L, JobStatus.PROCESSING, 0);
        when(jobRepository.findById(1L)).thenReturn(java.util.Optional.of(paused));

        jobService.markAsPaused(1L, "Timeout ao consultar a estação 21780250");

        assertThat(paused.getErrorMessage()).isEqualTo("Timeout ao consultar a estação 21780250");
    }

    @Test
    @DisplayName("Mensagem nula não quebra o registro")
    void nullErrorMessageIsSafe() {
        HistoricalDataJobEntity paused = job(1L, JobStatus.PROCESSING, 0);
        when(jobRepository.findById(1L)).thenReturn(java.util.Optional.of(paused));

        jobService.markAsPaused(1L, null);

        assertThat(paused.getStatus()).isEqualTo(JobStatus.PAUSED);
        assertThat(paused.getErrorMessage()).isNull();
    }

    // ------------------------------------------------------------- fixtures
    private HistoricalDataJobEntity job(Long id, JobStatus status, int retryCount) {
        HistoricalDataJobEntity job = new HistoricalDataJobEntity();
        job.setId(id);
        job.setStatus(status);
        job.setRetryCount(retryCount);
        job.setInstrumentId(1377L);
        job.setInstrumentName("EST TELEMETRICA");
        job.setCreatedReadings(0);
        job.setSkippedDays(0);
        return job;
    }
}
