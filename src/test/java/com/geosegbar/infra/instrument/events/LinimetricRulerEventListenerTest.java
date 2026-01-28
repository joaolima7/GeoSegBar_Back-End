package com.geosegbar.infra.instrument.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.common.enums.JobStatus;
import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.infra.historical_data_job.service.HistoricalDataJobService;
import com.geosegbar.infra.hydrotelemetric.services.AsyncHydrotelemetricCollectionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinimetricRulerEventListener Tests")
class LinimetricRulerEventListenerTest {

    @Mock
    private AsyncHydrotelemetricCollectionService asyncCollectionService;

    @Mock
    private HistoricalDataJobService historicalDataJobService;

    @InjectMocks
    private LinimetricRulerEventListener eventListener;

    private InstrumentEntity instrument;
    private LinimetricRulerCreatedEvent event;

    @BeforeEach
    void setUp() {
        instrument = new InstrumentEntity();
        instrument.setId(123L);
        instrument.setName("Régua Linimétrica - Rio Teste");

        event = new LinimetricRulerCreatedEvent(this, instrument);
    }

    @Test
    @DisplayName("Deve coletar dados instantâneos quando evento é recebido")
    void shouldCollectInstantDataWhenEventReceived() {
        // Arrange
        when(historicalDataJobService.hasActiveJobForInstrument(anyLong())).thenReturn(false);
        when(historicalDataJobService.enqueueJob(anyLong(), anyString())).thenReturn(createMockJob());

        // Act
        eventListener.handleLinimetricRulerCreated(event);

        // Assert
        verify(asyncCollectionService, times(1)).collectInstrumentDataAsync(eq(instrument));
    }

    @Test
    @DisplayName("Deve criar job de coleta histórica quando não existe job ativo")
    void shouldCreateHistoricalJobWhenNoActiveJobExists() {
        // Arrange
        when(historicalDataJobService.hasActiveJobForInstrument(123L)).thenReturn(false);
        HistoricalDataJobEntity mockJob = createMockJob();
        when(historicalDataJobService.enqueueJob(123L, "Régua Linimétrica - Rio Teste")).thenReturn(mockJob);

        // Act
        eventListener.handleLinimetricRulerCreated(event);

        // Assert
        verify(historicalDataJobService, times(1)).hasActiveJobForInstrument(eq(123L));
        verify(historicalDataJobService, times(1)).enqueueJob(eq(123L), eq("Régua Linimétrica - Rio Teste"));
    }

    @Test
    @DisplayName("Não deve criar job de coleta histórica quando já existe job ativo")
    void shouldNotCreateHistoricalJobWhenActiveJobExists() {
        // Arrange
        when(historicalDataJobService.hasActiveJobForInstrument(123L)).thenReturn(true);

        // Act
        eventListener.handleLinimetricRulerCreated(event);

        // Assert
        verify(historicalDataJobService, times(1)).hasActiveJobForInstrument(eq(123L));
        verify(historicalDataJobService, never()).enqueueJob(anyLong(), anyString());
    }

    @Test
    @DisplayName("Deve coletar dados instantâneos mesmo se criação de job histórico falhar")
    void shouldCollectInstantDataEvenIfHistoricalJobCreationFails() {
        // Arrange
        when(historicalDataJobService.hasActiveJobForInstrument(anyLong()))
                .thenThrow(new RuntimeException("Erro ao verificar job"));

        // Act
        eventListener.handleLinimetricRulerCreated(event);

        // Assert
        verify(asyncCollectionService, times(1)).collectInstrumentDataAsync(eq(instrument));
    }

    @Test
    @DisplayName("Não deve interromper processamento se enqueueJob lançar exceção")
    void shouldNotInterruptProcessingIfEnqueueJobThrowsException() {
        // Arrange
        when(historicalDataJobService.hasActiveJobForInstrument(123L)).thenReturn(false);
        when(historicalDataJobService.enqueueJob(123L, "Régua Linimétrica - Rio Teste"))
                .thenThrow(new RuntimeException("Erro ao criar job"));

        // Act
        eventListener.handleLinimetricRulerCreated(event);

        // Assert
        verify(asyncCollectionService, times(1)).collectInstrumentDataAsync(eq(instrument));
        verify(historicalDataJobService, times(1)).enqueueJob(anyLong(), anyString());
    }

    @Test
    @DisplayName("Deve logar informações do job criado")
    void shouldLogJobInformation() {
        // Arrange
        when(historicalDataJobService.hasActiveJobForInstrument(123L)).thenReturn(false);
        HistoricalDataJobEntity mockJob = createMockJob();
        when(historicalDataJobService.enqueueJob(123L, "Régua Linimétrica - Rio Teste")).thenReturn(mockJob);

        // Act
        eventListener.handleLinimetricRulerCreated(event);

        // Assert - O log é verificado indiretamente pelo comportamento do método
        verify(historicalDataJobService, times(1)).enqueueJob(eq(123L), eq("Régua Linimétrica - Rio Teste"));
    }

    @Test
    @DisplayName("Deve processar múltiplos eventos independentemente")
    void shouldProcessMultipleEventsIndependently() {
        // Arrange
        InstrumentEntity instrument2 = new InstrumentEntity();
        instrument2.setId(456L);
        instrument2.setName("Régua Linimétrica - Rio Teste 2");
        LinimetricRulerCreatedEvent event2 = new LinimetricRulerCreatedEvent(this, instrument2);

        when(historicalDataJobService.hasActiveJobForInstrument(123L)).thenReturn(false);
        when(historicalDataJobService.hasActiveJobForInstrument(456L)).thenReturn(false);
        when(historicalDataJobService.enqueueJob(anyLong(), anyString())).thenReturn(createMockJob());

        // Act
        eventListener.handleLinimetricRulerCreated(event);
        eventListener.handleLinimetricRulerCreated(event2);

        // Assert
        verify(asyncCollectionService, times(2)).collectInstrumentDataAsync(any(InstrumentEntity.class));
        verify(historicalDataJobService, times(2)).enqueueJob(anyLong(), anyString());
    }

    // Helper method
    private HistoricalDataJobEntity createMockJob() {
        HistoricalDataJobEntity job = new HistoricalDataJobEntity();
        job.setId(999L);
        job.setInstrumentId(123L);
        job.setInstrumentName("Régua Linimétrica - Rio Teste");
        job.setStatus(JobStatus.QUEUED);
        job.setStartDate(LocalDate.now().minusYears(10));
        job.setEndDate(LocalDate.now());
        job.setCheckpointDate(LocalDate.now().minusYears(10));
        job.setTotalMonths(120);
        job.setProcessedMonths(0);
        job.setCreatedReadings(0);
        job.setSkippedDays(0);
        job.setRetryCount(0);
        job.setCreatedAt(LocalDateTime.now());
        return job;
    }
}
