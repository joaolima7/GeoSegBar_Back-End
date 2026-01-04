package com.geosegbar.unit.infra.anomaly_status.services;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;
import com.geosegbar.infra.anomaly_status.services.AnomalyStatusService;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AnomalyStatusServiceTest {

    @Mock
    private AnomalyStatusRepository anomalyStatusRepository;

    @InjectMocks
    private AnomalyStatusService anomalyStatusService;

    private AnomalyStatusEntity mockStatus;

    @BeforeEach
    void setUp() {
        mockStatus = new AnomalyStatusEntity();
        mockStatus.setId(1L);
        mockStatus.setName("Pendente");
        mockStatus.setDescription("Anomalia identificada, aguardando análise");
    }

    @Test
    void shouldInitializeDefaultStatusesWhenNoneExist() {
        when(anomalyStatusRepository.findByName("Pendente")).thenReturn(Optional.empty());
        when(anomalyStatusRepository.findByName("Em andamento")).thenReturn(Optional.empty());
        when(anomalyStatusRepository.findByName("Concluído")).thenReturn(Optional.empty());
        when(anomalyStatusRepository.findByName("Em monitoramento")).thenReturn(Optional.empty());
        when(anomalyStatusRepository.findByName("--")).thenReturn(Optional.empty());

        anomalyStatusService.initializeDefaultStatus();

        verify(anomalyStatusRepository, times(5)).save(any(AnomalyStatusEntity.class));
    }

    @Test
    void shouldNotCreateDuplicateStatusWhenAlreadyExists() {
        when(anomalyStatusRepository.findByName("Pendente")).thenReturn(Optional.of(mockStatus));
        when(anomalyStatusRepository.findByName("Em andamento")).thenReturn(Optional.empty());
        when(anomalyStatusRepository.findByName("Concluído")).thenReturn(Optional.empty());
        when(anomalyStatusRepository.findByName("Em monitoramento")).thenReturn(Optional.empty());
        when(anomalyStatusRepository.findByName("--")).thenReturn(Optional.empty());

        anomalyStatusService.initializeDefaultStatus();

        verify(anomalyStatusRepository, times(4)).save(any(AnomalyStatusEntity.class));
    }

    @Test
    void shouldNotSaveAnyStatusWhenAllExist() {
        when(anomalyStatusRepository.findByName("Pendente")).thenReturn(Optional.of(mockStatus));
        when(anomalyStatusRepository.findByName("Em andamento")).thenReturn(Optional.of(mockStatus));
        when(anomalyStatusRepository.findByName("Concluído")).thenReturn(Optional.of(mockStatus));
        when(anomalyStatusRepository.findByName("Em monitoramento")).thenReturn(Optional.of(mockStatus));
        when(anomalyStatusRepository.findByName("--")).thenReturn(Optional.of(mockStatus));

        anomalyStatusService.initializeDefaultStatus();

        verify(anomalyStatusRepository, never()).save(any(AnomalyStatusEntity.class));
    }

    @Test
    void shouldFindAllStatuses() {
        List<AnomalyStatusEntity> statuses = List.of(mockStatus);
        when(anomalyStatusRepository.findAll()).thenReturn(statuses);

        List<AnomalyStatusEntity> result = anomalyStatusService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Pendente");
        verify(anomalyStatusRepository).findAll();
    }

    @Test
    void shouldFindStatusById() {
        when(anomalyStatusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));

        AnomalyStatusEntity result = anomalyStatusService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Pendente");
        assertThat(result.getDescription()).isEqualTo("Anomalia identificada, aguardando análise");
        verify(anomalyStatusRepository).findById(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenStatusNotFoundById() {
        when(anomalyStatusRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyStatusService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Anomaly status not found!");
    }

    @Test
    void shouldFindStatusByName() {
        when(anomalyStatusRepository.findByName("Pendente")).thenReturn(Optional.of(mockStatus));

        AnomalyStatusEntity result = anomalyStatusService.findByName("Pendente");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Pendente");
        verify(anomalyStatusRepository).findByName("Pendente");
    }

    @Test
    void shouldThrowNotFoundExceptionWhenStatusNotFoundByName() {
        when(anomalyStatusRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyStatusService.findByName("NonExistent"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Anomaly status not found with name: NonExistent");
    }

    @Test
    void shouldFindStatusByNameEmAndamento() {
        AnomalyStatusEntity emAndamento = new AnomalyStatusEntity();
        emAndamento.setId(2L);
        emAndamento.setName("Em andamento");
        emAndamento.setDescription("Anomalia em processo de tratamento");

        when(anomalyStatusRepository.findByName("Em andamento")).thenReturn(Optional.of(emAndamento));

        AnomalyStatusEntity result = anomalyStatusService.findByName("Em andamento");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Em andamento");
        assertThat(result.getDescription()).isEqualTo("Anomalia em processo de tratamento");
    }

    @Test
    void shouldFindStatusByNameConcluido() {
        AnomalyStatusEntity concluido = new AnomalyStatusEntity();
        concluido.setId(3L);
        concluido.setName("Concluído");
        concluido.setDescription("Tratamento da anomalia finalizado");

        when(anomalyStatusRepository.findByName("Concluído")).thenReturn(Optional.of(concluido));

        AnomalyStatusEntity result = anomalyStatusService.findByName("Concluído");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Concluído");
    }

    @Test
    void shouldFindStatusByNameEmMonitoramento() {
        AnomalyStatusEntity emMonitoramento = new AnomalyStatusEntity();
        emMonitoramento.setId(4L);
        emMonitoramento.setName("Em monitoramento");
        emMonitoramento.setDescription("Anomalia tratada mas sob observação");

        when(anomalyStatusRepository.findByName("Em monitoramento")).thenReturn(Optional.of(emMonitoramento));

        AnomalyStatusEntity result = anomalyStatusService.findByName("Em monitoramento");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Em monitoramento");
        assertThat(result.getDescription()).isEqualTo("Anomalia tratada mas sob observação");
    }

    @Test
    void shouldFindStatusByNameUndefined() {
        AnomalyStatusEntity undefined = new AnomalyStatusEntity();
        undefined.setId(5L);
        undefined.setName("--");
        undefined.setDescription("Ainda não foi definido um status para a anomalia");

        when(anomalyStatusRepository.findByName("--")).thenReturn(Optional.of(undefined));

        AnomalyStatusEntity result = anomalyStatusService.findByName("--");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("--");
        assertThat(result.getDescription()).isEqualTo("Ainda não foi definido um status para a anomalia");
    }

    @Test
    void shouldHandlePortugueseCharactersInStatusName() {
        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setId(10L);
        status.setName("Situação Crítica");
        status.setDescription("Anomalia em situação crítica requer ação imediata");

        when(anomalyStatusRepository.findByName("Situação Crítica")).thenReturn(Optional.of(status));

        AnomalyStatusEntity result = anomalyStatusService.findByName("Situação Crítica");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Situação Crítica");
    }

    @Test
    void shouldFindEmptyListWhenNoStatusesExist() {
        when(anomalyStatusRepository.findAll()).thenReturn(List.of());

        List<AnomalyStatusEntity> result = anomalyStatusService.findAll();

        assertThat(result).isEmpty();
        verify(anomalyStatusRepository).findAll();
    }

    @Test
    void shouldFindMultipleStatuses() {
        AnomalyStatusEntity status1 = new AnomalyStatusEntity();
        status1.setId(1L);
        status1.setName("Pendente");

        AnomalyStatusEntity status2 = new AnomalyStatusEntity();
        status2.setId(2L);
        status2.setName("Em andamento");

        AnomalyStatusEntity status3 = new AnomalyStatusEntity();
        status3.setId(3L);
        status3.setName("Concluído");

        when(anomalyStatusRepository.findAll()).thenReturn(List.of(status1, status2, status3));

        List<AnomalyStatusEntity> result = anomalyStatusService.findAll();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(AnomalyStatusEntity::getName)
                .containsExactly("Pendente", "Em andamento", "Concluído");
    }
}
