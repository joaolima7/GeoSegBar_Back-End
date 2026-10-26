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

    /**
     * Linha de ReadingRepository.findLatestGroupedReadingRowsByClientId, na
     * ordem de colunas documentada lá.
     */
    private static Object[] row(long readingId, long instrumentId, long damId, String date, String hour,
            String inputAcronym, Double inputValue) {
        return new Object[]{
            readingId, instrumentId, Date.valueOf(date), Time.valueOf(hour),
            new java.math.BigDecimal("1.5"), "NORMAL", null, true,
            7L, "Cota", "COTA",
            null, null, null,
            inputAcronym, inputAcronym == null ? null : "Leitura " + inputAcronym,
            inputValue == null ? null : new java.math.BigDecimal(inputValue.toString()),
            "PZ-" + instrumentId, "Piezômetro", damId, "Barragem " + damId};
    }

    @Test
    @DisplayName("últimas leituras agrupadas: barragem sem acesso fica fora da resposta")
    void latestGroupedAreRestrictedToAllowedDams() {
        when(readingRepository.findLatestGroupedReadingRowsByClientId(1L, 3)).thenReturn(List.of(
                row(100L, 1L, 10L, "2026-10-01", "09:00:00", "L", 1.0),
                row(200L, 2L, 20L, "2026-10-01", "10:00:00", "L", 2.0)));
        when(damAccessService.listingRestriction()).thenReturn(Optional.of(Set.of(10L)));

        List<InstrumentGroupedReadingsDTO> result = service.findLatestGroupedReadingsByClientId(1L, 3);

        assertThat(result).extracting(InstrumentGroupedReadingsDTO::getInstrumentId).containsExactly(1L);
        assertThat(result.get(0).getGroupedReadings()).hasSize(1);
        assertThat(result.get(0).getGroupedReadings().get(0).getReadings())
                .extracting(r -> r.getId()).containsExactly(100L);
    }

    @Test
    @DisplayName("ADMIN continua vendo as leituras de todas as barragens")
    void adminSeesEverything() {
        when(readingRepository.findLatestGroupedReadingRowsByClientId(1L, 3)).thenReturn(List.<Object[]>of(
                row(200L, 2L, 20L, "2026-10-01", "10:00:00", null, null)));
        when(damAccessService.listingRestriction()).thenReturn(Optional.empty());

        assertThat(service.findLatestGroupedReadingsByClientId(1L, 3))
                .extracting(InstrumentGroupedReadingsDTO::getInstrumentId).containsExactly(2L);
    }

    @Test
    @DisplayName("últimas leituras agrupadas: datas na ordem da consulta, entradas somadas por leitura")
    void latestGroupedAssemblesGroupsAndInputs() {
        when(readingRepository.findLatestGroupedReadingRowsByClientId(1L, 3)).thenReturn(List.of(
                // leitura 101 (mais recente) com duas entradas, leitura 102 da mesma data/hora sem entrada
                row(101L, 1L, 10L, "2026-10-02", "08:30:00", "L1", 10.25),
                row(101L, 1L, 10L, "2026-10-02", "08:30:00", "L2", 20.5),
                row(102L, 1L, 10L, "2026-10-02", "08:30:00", null, null),
                // data mais antiga
                row(90L, 1L, 10L, "2026-10-01", "09:00:00", "L1", 9.0)));
        when(damAccessService.listingRestriction()).thenReturn(Optional.empty());

        List<InstrumentGroupedReadingsDTO> result = service.findLatestGroupedReadingsByClientId(1L, 3);

        assertThat(result).hasSize(1);
        InstrumentGroupedReadingsDTO instrumento = result.get(0);
        assertThat(instrumento.getInstrumentName()).isEqualTo("PZ-1");
        assertThat(instrumento.getInstrumentType()).isEqualTo("Piezômetro");
        assertThat(instrumento.getDamId()).isEqualTo(10L);
        assertThat(instrumento.getDamName()).isEqualTo("Barragem 10");
        assertThat(instrumento.getGroupedReadings())
                .extracting(g -> g.getDateTime())
                .containsExactly("2026-10-02 08:30", "2026-10-01 09:00");

        var recente = instrumento.getGroupedReadings().get(0).getReadings();
        assertThat(recente).extracting(r -> r.getId()).containsExactly(101L, 102L);
        assertThat(recente.get(0).getInputValues())
                .extracting(v -> v.getInputAcronym(), v -> v.getValue())
                .containsExactly(org.assertj.core.groups.Tuple.tuple("L1", 10.25),
                        org.assertj.core.groups.Tuple.tuple("L2", 20.5));
        assertThat(recente.get(1).getInputValues()).isEmpty();
        assertThat(recente.get(0).getCalculatedValue()).isEqualByComparingTo("1.5");
        assertThat(recente.get(0).getLimitStatus().name()).isEqualTo("NORMAL");
        assertThat(recente.get(0).getOutputAcronym()).isEqualTo("COTA");
        assertThat(recente.get(0).getInstrumentName()).isEqualTo("PZ-1");
        assertThat(recente.get(0).getCreatedBy()).isNull();
        assertThat(recente.get(0).getDate()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(recente.get(0).getHour()).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    @DisplayName("últimas leituras agrupadas: cliente sem leitura devolve lista vazia")
    void latestGroupedEmpty() {
        when(readingRepository.findLatestGroupedReadingRowsByClientId(1L, 3)).thenReturn(List.of());

        assertThat(service.findLatestGroupedReadingsByClientId(1L, 3)).isEmpty();
        verify(damAccessService, never()).listingRestriction();
    }

    @Test
    @DisplayName("últimas leituras agrupadas: nenhuma entidade de instrumento é carregada")
    void latestGroupedLoadsNoInstrumentEntity() {
        when(readingRepository.findLatestGroupedReadingRowsByClientId(1L, 3)).thenReturn(List.<Object[]>of(
                row(100L, 1L, 10L, "2026-10-01", "09:00:00", null, null)));
        when(damAccessService.listingRestriction()).thenReturn(Optional.empty());

        service.findLatestGroupedReadingsByClientId(1L, 3);

        Mockito.verifyNoInteractions(instrumentRepository);
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
