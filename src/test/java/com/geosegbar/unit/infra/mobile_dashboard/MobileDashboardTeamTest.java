package com.geosegbar.unit.infra.mobile_dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
import com.geosegbar.entities.UserEntity;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.mobile_dashboard.dtos.MobileDashboardDTO;
import com.geosegbar.infra.mobile_dashboard.projections.DamInspectionProjection;
import com.geosegbar.infra.mobile_dashboard.projections.MonthlyCountProjection;
import com.geosegbar.infra.mobile_dashboard.services.MobileDashboardService;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;

/**
 * O painel mostra "você" e "equipe" lado a lado (V5-B02). Equipe = todos os
 * colaboradores das barragens que o usuário acessa, ele incluído.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("MobileDashboardService — você e equipe")
class MobileDashboardTeamTest {

    @Mock
    private DamRepository damRepository;
    @Mock
    private ChecklistResponseRepository checklistResponseRepository;
    @Mock
    private ReadingRepository readingRepository;
    @Mock
    private InstrumentRepository instrumentRepository;
    @Mock
    private AnomalyRepository anomalyRepository;

    @InjectMocks
    private MobileDashboardService service;

    private MockedStatic<AuthenticatedUserUtil> auth;
    private final String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        auth = Mockito.mockStatic(AuthenticatedUserUtil.class);
        auth.when(AuthenticatedUserUtil::getCurrentUser).thenReturn(user);
        auth.when(AuthenticatedUserUtil::isAdmin).thenReturn(false);
        when(damRepository.findAccessibleDamIdsByUserId(7L)).thenReturn(List.of(10L));

        lenient().when(readingRepository.countInstrumentsByLimitStatus(any(), any(), any())).thenReturn(List.of());
        lenient().when(readingRepository.findCriticalInstruments(any(), any(), any(), anyInt())).thenReturn(List.of());
        lenient().when(instrumentRepository.countActiveByTypeForDams(any())).thenReturn(List.of());
        lenient().when(readingRepository.countMyReadingsByInstrumentType(anyLong(), any(), any(), any())).thenReturn(List.of());
        lenient().when(anomalyRepository.countByDangerLevelGrouped(any(), any(), any())).thenReturn(List.of());
        lenient().when(checklistResponseRepository.findInspectionPace(any(), any(), any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        auth.close();
    }

    private static MonthlyCountProjection month(String bucket, long total) {
        return new MonthlyCountProjection() {
            @Override
            public String getBucket() {
                return bucket;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }

    private static DamInspectionProjection dam(LocalDateTime last, String userName) {
        return new DamInspectionProjection() {
            @Override
            public Long getDamId() {
                return 10L;
            }

            @Override
            public String getDamName() {
                return "Barragem";
            }

            @Override
            public Long getTotalInPeriod() {
                return 3L;
            }

            @Override
            public LocalDateTime getLastResponseAt() {
                return last;
            }

            @Override
            public String getLastInspectionUserName() {
                return userName;
            }
        };
    }

    @Test
    @DisplayName("KPIs trazem o do usuário e o da equipe, e a série mensal os dois")
    void kpisAndSeriesCarryMineAndTeam() {
        when(checklistResponseRepository.countMyResponsesByMonth(eq(7L), any(), any(), any()))
                .thenReturn(List.of(month(currentMonth, 1)));
        when(readingRepository.countMyReadingsByMonth(eq(7L), any(), any(), any()))
                .thenReturn(List.of(month(currentMonth, 2)));
        when(checklistResponseRepository.countResponsesByMonth(eq(List.of(10L)), any(), any()))
                .thenReturn(List.of(month(currentMonth, 3)));
        when(readingRepository.countReadingsByMonth(eq(List.of(10L)), any(), any()))
                .thenReturn(List.of(month(currentMonth, 3)));
        when(checklistResponseRepository.findInspectionSummaryByDam(any(), any(), any()))
                .thenReturn(List.of(dam(LocalDateTime.now(), "Maria")));

        MobileDashboardDTO dto = service.build(6, 5);

        assertThat(dto.kpis().myChecklistsThisMonth()).isEqualTo(1);
        assertThat(dto.kpis().teamChecklistsThisMonth()).isEqualTo(3);
        assertThat(dto.kpis().myReadingsThisMonth()).isEqualTo(2);
        assertThat(dto.kpis().teamReadingsThisMonth()).isEqualTo(3);
        assertThat(dto.kpis().teamChecklistsInPeriod()).isEqualTo(3);
        assertThat(dto.kpis().teamReadingsInPeriod()).isEqualTo(3);

        MobileDashboardDTO.ActivityPoint last = dto.activityByMonth().get(dto.activityByMonth().size() - 1);
        assertThat(last.month()).isEqualTo(currentMonth);
        assertThat(last.checklists()).isEqualTo(1);
        assertThat(last.teamChecklists()).isEqualTo(3);
        assertThat(last.teamReadings()).isEqualTo(3);

        assertThat(dto.inspectionsByDam()).singleElement()
                .extracting(MobileDashboardDTO.DamInspection::lastInspectionUserName)
                .isEqualTo("Maria");
    }

    @Test
    @DisplayName("barragem nunca inspecionada não tem autor da última inspeção")
    void damNeverInspectedHasNoLastAuthor() {
        when(checklistResponseRepository.countMyResponsesByMonth(eq(7L), any(), any(), any())).thenReturn(List.of());
        when(readingRepository.countMyReadingsByMonth(eq(7L), any(), any(), any())).thenReturn(List.of());
        when(checklistResponseRepository.countResponsesByMonth(any(), any(), any())).thenReturn(List.of());
        when(readingRepository.countReadingsByMonth(any(), any(), any())).thenReturn(List.of());
        when(checklistResponseRepository.findInspectionSummaryByDam(any(), any(), any()))
                .thenReturn(List.of(dam(null, null)));

        MobileDashboardDTO dto = service.build(6, 5);

        assertThat(dto.kpis().teamChecklistsThisMonth()).isZero();
        assertThat(dto.activityByMonth()).hasSize(6).allMatch(p -> p.teamChecklists() == 0 && p.teamReadings() == 0);
        assertThat(dto.inspectionsByDam().get(0).lastInspectionUserName()).isNull();
    }
}
