package com.geosegbar.unit.infra.reading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.services.DamAccessService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.dtos.InstrumentGroupedReadingsDTO;
import com.geosegbar.infra.reading.dtos.InstrumentLimitStatusDTO;
import com.geosegbar.infra.reading.dtos.UpdateReadingRequestDTO;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;
import com.geosegbar.infra.reading.projections.InstrumentLimitStatusProjection;
import com.geosegbar.infra.reading.services.ReadingService;

/**
 * As listagens de leitura do cliente respeitam o acesso à barragem, e corrigir
 * uma leitura exige o acesso à barragem do instrumento (V5-B01/B04). A criação
 * fica de fora de propósito: o app da main lê instrumentos do cliente inteiro.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("ReadingService — acesso à barragem")
class ReadingServiceAccessTest {

    @Mock
    private ReadingRepository readingRepository;
    @Mock
    private InstrumentRepository instrumentRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private DamAccessService damAccessService;

    @InjectMocks
    private ReadingService service;

    private MockedStatic<AuthenticatedUserUtil> auth;
    private ClientEntity client;

    @BeforeEach
    void setUp() {
        client = new ClientEntity();
        client.setId(1L);
        client.setName("Cliente");
        auth = Mockito.mockStatic(AuthenticatedUserUtil.class);
        auth.when(AuthenticatedUserUtil::isAdmin).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        auth.close();
    }

    private InstrumentEntity instrument(long id, long damId) {
        DamEntity dam = new DamEntity();
        dam.setId(damId);
        dam.setName("Barragem " + damId);
        dam.setClient(client);
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(3L);
        type.setName("Piezômetro");
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(id);
        instrument.setName("PZ-" + id);
        instrument.setDam(dam);
        instrument.setInstrumentType(type);
        return instrument;
    }

    private InstrumentLimitStatusProjection projection(long instrumentId, long damId) {
        InstrumentLimitStatusProjection p = mock(InstrumentLimitStatusProjection.class);
        lenient().when(p.getInstrumentId()).thenReturn(instrumentId);
        lenient().when(p.getDamId()).thenReturn(damId);
        lenient().when(p.getReadingDate()).thenReturn(LocalDate.of(2026, 10, 1));
        lenient().when(p.getReadingHour()).thenReturn(LocalTime.of(9, 0));
        lenient().when(p.getLimitStatus()).thenReturn("NORMAL");
        return p;
    }

    @Test
    @DisplayName("status de limite traz só instrumentos das barragens acessíveis")
    void limitStatusesAreRestricted() {
        when(clientRepository.existsById(1L)).thenReturn(true);
        when(damAccessService.listingRestriction()).thenReturn(Optional.of(Set.of(10L)));
        List<InstrumentLimitStatusProjection> projections = List.of(projection(1L, 10L), projection(2L, 20L));
        when(readingRepository.findLatestLimitStatusByClientId(1L, 10)).thenReturn(projections);
        when(instrumentRepository.findByFiltersOptimized(null, null, null, true, 1L))
                .thenReturn(List.of(instrument(1L, 10L), instrument(2L, 20L), instrument(3L, 10L)));

        List<InstrumentLimitStatusDTO> result = service.getAllInstrumentLimitStatusesByClientId(1L, 10);

        assertThat(result).extracting(InstrumentLimitStatusDTO::getInstrumentId)
                .containsExactlyInAnyOrder(1L, 3L);
        assertThat(result).allMatch(dto -> dto.getDamId() == 10L);
    }

    @Test
    @DisplayName("sem barragem acessível, status de limite vazio sem consultar leituras")
    void limitStatusesWithoutAccessAreEmpty() {
        when(clientRepository.existsById(1L)).thenReturn(true);
        when(damAccessService.listingRestriction()).thenReturn(Optional.of(Set.of()));

        assertThat(service.getAllInstrumentLimitStatusesByClientId(1L, 10)).isEmpty();
        verify(readingRepository, never()).findLatestLimitStatusByClientId(anyLong(), anyInt());
    }

    @Test
    @DisplayName("últimas leituras agrupadas cortam antes de carregar as leituras")
    void latestGroupedAreRestrictedBeforeLoadingReadings() {
        when(readingRepository.findLatestDistinctDateHoursByClientId(1L, 3)).thenReturn(List.of(
                new Object[]{1L, Date.valueOf("2026-10-01"), Time.valueOf("09:00:00")},
                new Object[]{2L, Date.valueOf("2026-10-01"), Time.valueOf("10:00:00")}));
        when(instrumentRepository.findAllById(any()))
                .thenReturn(List.of(instrument(1L, 10L), instrument(2L, 20L)));
        when(damAccessService.listingRestriction()).thenReturn(Optional.of(Set.of(10L)));
        when(readingRepository.findByInstrumentIdsAndActiveTrueWithAllRelations(List.of(1L)))
                .thenReturn(List.of());

        List<InstrumentGroupedReadingsDTO> result = service.findLatestGroupedReadingsByClientId(1L, 3);

        assertThat(result).extracting(InstrumentGroupedReadingsDTO::getInstrumentId).containsExactly(1L);
        verify(readingRepository).findByInstrumentIdsAndActiveTrueWithAllRelations(List.of(1L));
    }

    @Test
    @DisplayName("ADMIN continua vendo as leituras de todas as barragens")
    void adminSeesEverything() {
        when(readingRepository.findLatestDistinctDateHoursByClientId(1L, 3)).thenReturn(List.<Object[]>of(
                new Object[]{2L, Date.valueOf("2026-10-01"), Time.valueOf("10:00:00")}));
        when(instrumentRepository.findAllById(any())).thenReturn(List.of(instrument(2L, 20L)));
        when(damAccessService.listingRestriction()).thenReturn(Optional.empty());
        when(readingRepository.findByInstrumentIdsAndActiveTrueWithAllRelations(any())).thenReturn(List.of());

        assertThat(service.findLatestGroupedReadingsByClientId(1L, 3))
                .extracting(InstrumentGroupedReadingsDTO::getInstrumentId).containsExactly(2L);
    }

    @Test
    @DisplayName("corrigir leitura de barragem sem acesso é 403 e não grava")
    void updateReadingWithoutDamAccessIsForbidden() {
        ReadingEntity reading = new ReadingEntity();
        reading.setId(50L);
        reading.setActive(true);
        reading.setDate(LocalDate.of(2026, 10, 1));
        reading.setHour(LocalTime.of(9, 0));
        reading.setInstrument(instrument(2L, 20L));
        when(readingRepository.findByIdWithAllRelations(50L)).thenReturn(Optional.of(reading));
        doThrow(new ForbiddenException("sem acesso")).when(damAccessService).requireAccess(20L);

        assertThatThrownBy(() -> service.updateReading(50L, new UpdateReadingRequestDTO()))
                .isInstanceOf(ForbiddenException.class);
        verify(readingRepository, never()).save(any());
    }
}
