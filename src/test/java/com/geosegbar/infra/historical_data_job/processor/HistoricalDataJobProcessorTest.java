package com.geosegbar.infra.historical_data_job.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.common.enums.JobStatus;
import com.geosegbar.common.response.AnaTelemetryResponse.TelemetryItem;
import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.exceptions.ExternalApiException;
import com.geosegbar.infra.historical_data_job.service.HistoricalDataJobService;
import com.geosegbar.infra.hydrotelemetric.services.AnaApiService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.services.ReadingService;

/**
 * Testes unitários para HistoricalDataJobProcessor
 */
@ExtendWith(MockitoExtension.class)
class HistoricalDataJobProcessorTest {

    @Mock
    private HistoricalDataJobService jobService;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private AnaApiService anaApiService;

    @Mock
    private ReadingService readingService;

    @InjectMocks
    private HistoricalDataJobProcessor processor;

    private HistoricalDataJobEntity job;
    private InstrumentEntity instrument;

    @BeforeEach
    void setUp() {
        // Job padrão
        job = new HistoricalDataJobEntity();
        job.setId(1L);
        job.setInstrumentId(100L);
        job.setInstrumentName("Régua Teste");
        job.setStatus(JobStatus.QUEUED);
        job.setStartDate(LocalDate.of(2024, 1, 1));
        job.setEndDate(LocalDate.of(2024, 1, 5)); // Apenas 5 dias para teste
        job.setCheckpointDate(LocalDate.of(2024, 1, 1));
        job.setTotalMonths(1);

        // Instrumento padrão
        instrument = new InstrumentEntity();
        instrument.setId(100L);
        instrument.setName("Régua Teste");
        instrument.setLinimetricRulerCode(12345678L);
    }

    @Test
    @DisplayName("Deve processar job com sucesso")
    void shouldProcessJobSuccessfully() throws Exception {
        // Given
        when(jobService.findById(1L)).thenReturn(Optional.of(job));
        when(instrumentRepository.findById(100L)).thenReturn(Optional.of(instrument));
        when(anaApiService.getAuthToken()).thenReturn("token123");
        when(anaApiService.getTelemetryData(anyString(), anyString())).thenReturn(createMockTelemetryData());
        when(anaApiService.calculateAverageLevel(any(), any())).thenReturn(1500.0); // 1.5m
        when(readingService.existsByInstrumentAndDate(anyLong(), any())).thenReturn(false);
        doNothing().when(readingService).create(anyLong(), any(ReadingRequestDTO.class), anyBoolean());

        // When
        CompletableFuture<Void> result = processor.processJob(1L);

        // Then
        assertNotNull(result);
        // Aguarda até 5 segundos para conclusão
        assertDoesNotThrow(() -> result.get(java.util.concurrent.TimeUnit.SECONDS.toMillis(5),
                java.util.concurrent.TimeUnit.MILLISECONDS));

        verify(jobService).markAsProcessing(1L);
        verify(jobService, atLeastOnce()).updateProgress(eq(1L), any(), anyInt(), anyInt());
        verify(jobService).markAsCompleted(1L);
        verify(anaApiService).getAuthToken();
        verify(readingService, atLeast(5)).create(eq(100L), any(), eq(true));
    }

    @Test
    @DisplayName("Deve lançar exceção quando job não existe")
    void shouldThrowExceptionWhenJobNotFound() throws Exception {
        // Given
        when(jobService.findById(1L)).thenReturn(Optional.empty());

        // When
        CompletableFuture<Void> result = processor.processJob(1L);

        // Then
        Exception exception = assertThrows(Exception.class,
                () -> result.get(java.util.concurrent.TimeUnit.SECONDS.toMillis(2),
                        java.util.concurrent.TimeUnit.MILLISECONDS));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("Deve lançar exceção quando instrumento não possui código linimétrico")
    void shouldThrowExceptionWhenInstrumentHasNoLinimetricCode() throws Exception {
        // Given
        instrument.setLinimetricRulerCode(null);
        when(jobService.findById(1L)).thenReturn(Optional.of(job));
        when(instrumentRepository.findById(100L)).thenReturn(Optional.of(instrument));

        // When
        CompletableFuture<Void> result = processor.processJob(1L);

        // Then
        Exception exception = assertThrows(Exception.class,
                () -> result.get(java.util.concurrent.TimeUnit.SECONDS.toMillis(2),
                        java.util.concurrent.TimeUnit.MILLISECONDS));
        assertTrue(exception.getCause() instanceof IllegalStateException);
    }

    @Test
    @DisplayName("Deve pausar job em caso de erro da API externa")
    void shouldPauseJobOnExternalApiError() throws Exception {
        // Given
        when(jobService.findById(1L)).thenReturn(Optional.of(job));
        when(instrumentRepository.findById(100L)).thenReturn(Optional.of(instrument));
        when(anaApiService.getAuthToken()).thenThrow(new ExternalApiException("Token expirado"));
        when(jobService.incrementRetry(1L)).thenReturn(true); // Pode tentar novamente

        // When
        CompletableFuture<Void> result = processor.processJob(1L);

        // Then
        assertThrows(Exception.class,
                () -> result.get(java.util.concurrent.TimeUnit.SECONDS.toMillis(2),
                        java.util.concurrent.TimeUnit.MILLISECONDS));
        verify(jobService).markAsProcessing(1L);
        verify(jobService).incrementRetry(1L);
        verify(jobService).markAsPaused(eq(1L), contains("Token expirado"));
        verify(jobService, never()).markAsCompleted(anyLong());
    }

    @Test
    @DisplayName("Deve falhar job após 3 tentativas de erro da API")
    void shouldFailJobAfterMaxRetries() throws Exception {
        // Given
        when(jobService.findById(1L)).thenReturn(Optional.of(job));
        when(instrumentRepository.findById(100L)).thenReturn(Optional.of(instrument));
        when(anaApiService.getAuthToken()).thenThrow(new ExternalApiException("Erro permanente"));
        when(jobService.incrementRetry(1L)).thenReturn(false); // Não pode mais tentar

        // When
        CompletableFuture<Void> result = processor.processJob(1L);

        // Then
        assertThrows(Exception.class,
                () -> result.get(java.util.concurrent.TimeUnit.SECONDS.toMillis(2),
                        java.util.concurrent.TimeUnit.MILLISECONDS));
        verify(jobService).incrementRetry(1L);
        verify(jobService).markAsFailed(eq(1L), contains("Falhou após 3 tentativas"));
        verify(jobService, never()).markAsPaused(anyLong(), anyString());
    }

    @Test
    @DisplayName("Deve pular dias sem dados ou duplicados")
    void shouldSkipDaysWithoutDataOrDuplicates() throws Exception {
        // Given
        when(jobService.findById(1L)).thenReturn(Optional.of(job));
        when(instrumentRepository.findById(100L)).thenReturn(Optional.of(instrument));
        when(anaApiService.getAuthToken()).thenReturn("token123");
        when(anaApiService.getTelemetryData(anyString(), anyString())).thenReturn(createMockTelemetryData());

        // Dias 1 e 3 com dados, dias 2, 4, 5 sem dados
        when(anaApiService.calculateAverageLevel(any(), any()))
                .thenReturn(1500.0) // Dia 1
                .thenReturn(null) // Dia 2 - sem dados
                .thenReturn(1600.0) // Dia 3
                .thenReturn(0.0) // Dia 4 - zero
                .thenReturn(null);   // Dia 5 - sem dados

        when(readingService.existsByInstrumentAndDate(anyLong(), any())).thenReturn(false);
        doNothing().when(readingService).create(anyLong(), any(ReadingRequestDTO.class), anyBoolean());

        // When
        CompletableFuture<Void> result = processor.processJob(1L);

        // Then
        assertDoesNotThrow(() -> result.get());

        // Deve criar apenas 2 readings (dias 1 e 3)
        verify(readingService, times(2)).create(eq(100L), any(), eq(true));
        verify(jobService, atLeastOnce()).updateProgress(eq(1L), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Deve marcar job como falho em erro genérico")
    void shouldFailJobOnGenericError() throws Exception {
        // Given
        when(jobService.findById(1L)).thenReturn(Optional.of(job));
        when(instrumentRepository.findById(100L)).thenReturn(Optional.of(instrument));
        when(anaApiService.getAuthToken()).thenReturn("token123");
        when(anaApiService.getTelemetryData(anyString(), anyString()))
                .thenThrow(new RuntimeException("Erro inesperado"));

        // When
        CompletableFuture<Void> result = processor.processJob(1L);

        // Then
        assertThrows(Exception.class, () -> result.get());
        verify(jobService).markAsFailed(eq(1L), contains("Erro inesperado"));
        verify(jobService, never()).incrementRetry(anyLong());
    }

    @Test
    @DisplayName("Deve truncar mensagem de erro se muito longa")
    void shouldTruncateLongErrorMessage() throws Exception {
        // Given
        String longError = "X".repeat(2500); // Maior que 2000 chars
        when(jobService.findById(1L)).thenReturn(Optional.of(job));
        when(instrumentRepository.findById(100L)).thenReturn(Optional.of(instrument));
        when(anaApiService.getAuthToken()).thenReturn("token123");
        when(anaApiService.getTelemetryData(anyString(), anyString()))
                .thenThrow(new RuntimeException(longError));

        // When
        CompletableFuture<Void> result = processor.processJob(1L);

        // Then
        assertThrows(Exception.class, () -> result.get());
        verify(jobService).markAsFailed(eq(1L), argThat(msg
                -> msg.length() <= 2000 && msg.endsWith("...")
        ));
    }

    /**
     * Helper para criar dados de telemetria mockados
     */
    private List<TelemetryItem> createMockTelemetryData() {
        List<TelemetryItem> items = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            TelemetryItem item = new TelemetryItem();
            item.setCodigoEstacao("12345678");
            item.setDataHoraMedicao("2024-01-0" + i + "T12:00:00");
            item.setCotaAdotada("1500");
            items.add(item);
        }
        return items;
    }
}
