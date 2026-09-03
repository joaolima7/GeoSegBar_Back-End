package com.geosegbar.infra.mobile_dashboard.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.dashboard.projections.CategoryCountProjection;
import com.geosegbar.infra.dashboard.projections.InstrumentTypeCountProjection;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.mobile_dashboard.dtos.MobileDashboardDTO;
import com.geosegbar.infra.mobile_dashboard.projections.CriticalInstrumentProjection;
import com.geosegbar.infra.mobile_dashboard.projections.DamInspectionProjection;
import com.geosegbar.infra.mobile_dashboard.projections.MonthlyCountProjection;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;

import lombok.RequiredArgsConstructor;

/**
 * O painel que o aplicativo abre.
 *
 * Três escolhas explicam o desenho:
 *
 * 1. O RECORTE NASCE NO SERVIDOR. Nenhum damId é aceito. As rotas de
 * /dashboard, feitas para a web, exigem a lista de barragens e devolvem 403 se
 * uma delas não for permitida — comportamento correto lá, inviável aqui: o app
 * é offline-first e guarda permissão em cache, então a lista que ele mandaria
 * está sempre a um passo de estar velha, e cada defasagem viraria uma tela de
 * erro em campo. Aqui a permissão é lida na hora, e quem não tem barragem
 * nenhuma recebe 200 zerado, não 403.
 *
 * 2. TUDO AGREGADO NO BANCO. São nove consultas de GROUP BY sobre colunas
 * indexadas. Nenhuma leitura, resposta ou anomalia individual trafega: a
 * resposta inteira fica na casa das poucas dezenas de linhas, o que importa
 * para um aparelho em rede de campo.
 *
 * 3. SEM CACHE, de propósito. As cinco rotas de /dashboard são @Cacheable por
 * 60s porque a web as recarrega em pouco tempo. No app existe puxar-para-
 * atualizar, e um gesto explícito do inspetor que devolvesse o mesmo número de
 * um minuto atrás seria pior que a espera da consulta — que é curta, porque é
 * agregada.
 */
@Service
@RequiredArgsConstructor
public class MobileDashboardService {

    /**
     * Ordem de exibição dos estados de limite: do normal ao mais grave. O app
     * desenha na ordem que recebe.
     */
    private static final List<String> LIMIT_STATUS_ORDER
            = List.of("NORMAL", "INFERIOR", "SUPERIOR", "ATENCAO", "ALERTA", "EMERGENCIA");

    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final DamRepository damRepository;
    private final ChecklistResponseRepository checklistResponseRepository;
    private final ReadingRepository readingRepository;
    private final InstrumentRepository instrumentRepository;
    private final AnomalyRepository anomalyRepository;

    @Transactional(readOnly = true)
    public MobileDashboardDTO build(int months, int criticalLimit) {
        UserEntity user = AuthenticatedUserUtil.getCurrentUser();

        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.withDayOfMonth(1).minusMonths(months - 1L);
        LocalDate periodEnd = today;
        LocalDate monthStart = today.withDayOfMonth(1);

        LocalDateTime periodStartTime = periodStart.atStartOfDay();
        LocalDateTime periodEndTime = periodEnd.atTime(LocalTime.MAX);
        LocalDateTime monthStartTime = monthStart.atStartOfDay();

        List<Long> damIds = accessibleDamIds(user);

        if (damIds.isEmpty()) {
            return MobileDashboardDTO.empty(
                    periodStart, periodEnd, denseSeries(periodStart, months, Map.of(), Map.of()));
        }

        // ---- as nove agregações ----
        Map<String, Long> checklistsByMonth = toMonthMap(
                checklistResponseRepository.countMyResponsesByMonth(
                        user.getId(), damIds, periodStartTime, periodEndTime));

        Map<String, Long> readingsByMonth = toMonthMap(
                readingRepository.countMyReadingsByMonth(
                        user.getId(), damIds, periodStart, periodEnd));

        List<DamInspectionProjection> damRows
                = checklistResponseRepository.findInspectionSummaryByDam(
                        damIds, periodStartTime, periodEndTime);

        List<CategoryCountProjection> statusRows
                = readingRepository.countInstrumentsByLimitStatus(damIds, periodStart, periodEnd);

        List<CriticalInstrumentProjection> criticalRows
                = readingRepository.findCriticalInstruments(
                        damIds, periodStart, periodEnd, criticalLimit);

        List<InstrumentTypeCountProjection> typeRows
                = instrumentRepository.countActiveByTypeForDams(damIds);

        List<InstrumentTypeCountProjection> myReadingTypeRows
                = readingRepository.countMyReadingsByInstrumentType(
                        user.getId(), damIds, periodStart, periodEnd);

        List<CategoryCountProjection> dangerRows
                = anomalyRepository.countByDangerLevelGrouped(damIds, periodStartTime, periodEndTime);

        // ---- montagem ----
        List<MobileDashboardDTO.ActivityPoint> activity
                = denseSeries(periodStart, months, checklistsByMonth, readingsByMonth);

        List<MobileDashboardDTO.DamInspection> inspections = damRows.stream()
                .map(row -> new MobileDashboardDTO.DamInspection(
                row.getDamId(),
                row.getDamName(),
                row.getTotalInPeriod() != null ? row.getTotalInPeriod() : 0L,
                row.getLastResponseAt(),
                row.getLastResponseAt() != null
                && !row.getLastResponseAt().isBefore(monthStartTime)))
                .sorted(MobileDashboardService::mostOverdueFirst)
                .toList();

        List<MobileDashboardDTO.CategoryCount> instrumentStatus
                = orderedStatusCategories(statusRows);

        List<MobileDashboardDTO.TypeCount> instrumentsByType = toTypeCounts(typeRows);
        List<MobileDashboardDTO.TypeCount> myReadingsByType = toTypeCounts(myReadingTypeRows);

        List<MobileDashboardDTO.CategoryCount> anomaliesByDangerLevel
                = toPercentageCategories(dangerRows);

        List<MobileDashboardDTO.CriticalInstrument> criticalInstruments = criticalRows.stream()
                .map(row -> new MobileDashboardDTO.CriticalInstrument(
                row.getInstrumentId(),
                row.getInstrumentName(),
                row.getDamId(),
                row.getDamName(),
                row.getLimitStatus(),
                row.getLastReadingDate()))
                .toList();

        String currentMonthKey = monthStart.format(MONTH_KEY);

        long instrumentsTotal = typeRows.stream()
                .mapToLong(t -> t.getTotal() != null ? t.getTotal() : 0L)
                .sum();
        long instrumentsWithReading = instrumentStatus.stream()
                .mapToLong(MobileDashboardDTO.CategoryCount::count)
                .sum();

        MobileDashboardDTO.Kpis kpis = new MobileDashboardDTO.Kpis(
                checklistsByMonth.getOrDefault(currentMonthKey, 0L),
                readingsByMonth.getOrDefault(currentMonthKey, 0L),
                sum(checklistsByMonth),
                sum(readingsByMonth),
                inspections.stream().filter(MobileDashboardDTO.DamInspection::inspectedThisMonth).count(),
                damIds.size(),
                instrumentsTotal,
                instrumentsWithReading,
                Math.max(0L, instrumentsTotal - instrumentsWithReading),
                countOf(instrumentStatus, "ATENCAO"),
                countOf(instrumentStatus, "ALERTA") + countOf(instrumentStatus, "EMERGENCIA"),
                dangerRows.stream().mapToLong(CategoryCountProjection::getCount).sum());

        return new MobileDashboardDTO(
                LocalDateTime.now(),
                periodStart,
                periodEnd,
                new MobileDashboardDTO.Scope(damIds.size(), instrumentsTotal, damIds),
                kpis,
                activity,
                inspections,
                instrumentStatus,
                instrumentsByType,
                myReadingsByType,
                anomaliesByDangerLevel,
                criticalInstruments);
    }

    /**
     * ADMIN enxerga tudo; os demais, o que dam_permissions liberar. Mesma regra
     * de /dams/accessible — reaproveitada, e não reescrita, para as duas rotas
     * nunca divergirem sobre o que o usuário pode ver.
     */
    private List<Long> accessibleDamIds(UserEntity user) {
        return AuthenticatedUserUtil.isAdmin()
                ? damRepository.findAllDamIds()
                : damRepository.findAccessibleDamIdsByUserId(user.getId());
    }

    /**
     * A série tem um ponto por mês do período, inclusive os meses em que o
     * inspetor não trabalhou. Buraco em gráfico de linha vira interpolação, e
     * interpolação aqui contaria uma história falsa: "fez alguma coisa" onde
     * não fez nada.
     */
    private List<MobileDashboardDTO.ActivityPoint> denseSeries(
            LocalDate periodStart, int months,
            Map<String, Long> checklists, Map<String, Long> readings) {

        List<MobileDashboardDTO.ActivityPoint> points = new ArrayList<>(months);
        YearMonth cursor = YearMonth.from(periodStart);

        for (int i = 0; i < months; i++) {
            String key = cursor.format(MONTH_KEY);
            points.add(new MobileDashboardDTO.ActivityPoint(
                    key,
                    cursor.atDay(1),
                    checklists.getOrDefault(key, 0L),
                    readings.getOrDefault(key, 0L)));
            cursor = cursor.plusMonths(1);
        }
        return points;
    }

    /**
     * Os seis estados sempre, na mesma ordem, mesmo os que deram zero. Uma
     * fatia que some entre uma atualização e outra faz o gráfico trocar de cor
     * e a legenda mudar de tamanho — e o inspetor lê isso como se o dado
     * tivesse mudado de natureza, não de valor.
     */
    private List<MobileDashboardDTO.CategoryCount> orderedStatusCategories(
            List<CategoryCountProjection> rows) {

        Map<String, Long> byName = rows.stream()
                .collect(Collectors.toMap(
                        CategoryCountProjection::getName,
                        CategoryCountProjection::getCount,
                        Long::sum));

        long total = byName.values().stream().mapToLong(Long::longValue).sum();

        return LIMIT_STATUS_ORDER.stream()
                .map(status -> new MobileDashboardDTO.CategoryCount(
                status, byName.getOrDefault(status, 0L),
                percentage(byName.getOrDefault(status, 0L), total)))
                .toList();
    }

    private List<MobileDashboardDTO.CategoryCount> toPercentageCategories(
            List<CategoryCountProjection> rows) {

        long total = rows.stream().mapToLong(CategoryCountProjection::getCount).sum();

        return rows.stream()
                .map(row -> new MobileDashboardDTO.CategoryCount(
                row.getName(), row.getCount(), percentage(row.getCount(), total)))
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .toList();
    }

    private List<MobileDashboardDTO.TypeCount> toTypeCounts(List<InstrumentTypeCountProjection> rows) {
        return rows.stream()
                .map(row -> new MobileDashboardDTO.TypeCount(
                row.getTypeId(),
                row.getTypeName(),
                row.getTotal() != null ? row.getTotal() : 0L))
                .toList();
    }

    private Map<String, Long> toMonthMap(List<MonthlyCountProjection> rows) {
        return rows.stream().collect(Collectors.toMap(
                MonthlyCountProjection::getBucket,
                row -> row.getTotal() != null ? row.getTotal() : 0L,
                Long::sum,
                LinkedHashMap::new));
    }

    private long sum(Map<String, Long> counts) {
        return counts.values().stream().mapToLong(Long::longValue).sum();
    }

    private long countOf(List<MobileDashboardDTO.CategoryCount> categories, String name) {
        return categories.stream()
                .filter(c -> c.name().equals(name))
                .mapToLong(MobileDashboardDTO.CategoryCount::count)
                .findFirst()
                .orElse(0L);
    }

    private double percentage(long count, long total) {
        return total > 0 ? Math.round(count * 1000.0 / total) / 10.0 : 0.0;
    }

    /**
     * Quem está mais atrasado aparece primeiro: primeiro as nunca
     * inspecionadas, depois da mais antiga para a mais recente. É a ordem em
     * que o inspetor decide para onde ir.
     */
    private static int mostOverdueFirst(
            MobileDashboardDTO.DamInspection a, MobileDashboardDTO.DamInspection b) {

        if (a.lastInspectionAt() == null && b.lastInspectionAt() == null) {
            return a.damName().compareToIgnoreCase(b.damName());
        }
        if (a.lastInspectionAt() == null) {
            return -1;
        }
        if (b.lastInspectionAt() == null) {
            return 1;
        }
        int byDate = a.lastInspectionAt().compareTo(b.lastInspectionAt());
        return byDate != 0 ? byDate : a.damName().compareToIgnoreCase(b.damName());
    }
}
