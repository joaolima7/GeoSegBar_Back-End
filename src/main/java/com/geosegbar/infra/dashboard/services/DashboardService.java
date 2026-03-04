package com.geosegbar.infra.dashboard.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.dashboard.dtos.CategoryCountDTO;
import com.geosegbar.infra.dashboard.dtos.DashboardCategorySummaryDTO;
import com.geosegbar.infra.dashboard.dtos.InstrumentDashboardSummaryDTO;
import com.geosegbar.infra.dashboard.dtos.InstrumentTypeDashboardDTO;
import com.geosegbar.infra.dashboard.projections.CategoryCountProjection;
import com.geosegbar.infra.dashboard.projections.InstrumentStatusDistributionProjection;
import com.geosegbar.infra.dashboard.projections.InstrumentTypeCountProjection;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.permissions.dam_permissions.persistence.DamPermissionRepository;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<String> STATUS_ORDER
            = List.of("NORMAL", "ATENCAO", "ALERTA", "EMERGENCIA", "SUPERIOR", "INFERIOR");

    private final AnomalyRepository anomalyRepository;
    private final InstrumentRepository instrumentRepository;
    private final ReadingRepository readingRepository;
    private final DamPermissionRepository damPermissionRepository;

    public void validateDamAccess(List<Long> damIds) {
        UserEntity user = AuthenticatedUserUtil.getCurrentUser();

        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }

        List<Long> accessibleDamIds
                = damPermissionRepository.findAccessibleDamIds(user.getId(), damIds);

        if (accessibleDamIds.size() != damIds.size()) {
            throw new ForbiddenException(
                    "Acesso negado. Você não tem permissão para acessar uma ou mais barragens solicitadas.");
        }
    }

    // ======================== ANOMALY ENDPOINTS ========================
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard-danger-level-summary",
            key = "#damIds.toString() + ':' + #startDate + ':' + #endDate")
    public DashboardCategorySummaryDTO getDangerLevelSummary(
            List<Long> damIds, LocalDate startDate, LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<CategoryCountProjection> counts
                = anomalyRepository.countByDangerLevelGrouped(damIds, start, end);

        return buildCategorySummary(counts);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard-anomaly-status-summary",
            key = "#damIds.toString() + ':' + #startDate + ':' + #endDate")
    public DashboardCategorySummaryDTO getAnomalyStatusSummary(
            List<Long> damIds, LocalDate startDate, LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<CategoryCountProjection> counts
                = anomalyRepository.countByStatusGrouped(damIds, start, end);

        return buildCategorySummary(counts);
    }

    // ======================== INSTRUMENT ENDPOINT ========================
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard-instrument-summary",
            key = "#damIds.toString() + ':' + #startDate + ':' + #endDate")
    public InstrumentDashboardSummaryDTO getInstrumentSummary(
            List<Long> damIds, LocalDate startDate, LocalDate endDate) {

        List<InstrumentTypeCountProjection> typeCounts
                = instrumentRepository.countActiveByTypeForDams(damIds);

        List<InstrumentStatusDistributionProjection> statusDist
                = readingRepository.findInstrumentStatusDistributionByType(
                        damIds, startDate, endDate);

        return buildInstrumentSummary(typeCounts, statusDist);
    }

    // ======================== BUILDERS ========================
    private DashboardCategorySummaryDTO buildCategorySummary(List<CategoryCountProjection> counts) {
        long total = counts.stream()
                .mapToLong(CategoryCountProjection::getCount)
                .sum();

        if (total == 0) {
            return new DashboardCategorySummaryDTO(0, Collections.emptyList());
        }

        List<CategoryCountDTO> categories = counts.stream()
                .map(c -> new CategoryCountDTO(
                c.getName(),
                c.getCount(),
                Math.round(c.getCount() * 1000.0 / total) / 10.0))
                .toList();

        return new DashboardCategorySummaryDTO(total, categories);
    }

    private InstrumentDashboardSummaryDTO buildInstrumentSummary(
            List<InstrumentTypeCountProjection> typeCounts,
            List<InstrumentStatusDistributionProjection> statusDist) {

        long totalInstruments = typeCounts.stream()
                .mapToLong(InstrumentTypeCountProjection::getTotal)
                .sum();
        int totalTypes = typeCounts.size();

        Map<Long, List<InstrumentStatusDistributionProjection>> statusByType = statusDist.stream()
                .collect(Collectors.groupingBy(InstrumentStatusDistributionProjection::getTypeId));

        Map<String, Long> overallStatusMap = statusDist.stream()
                .collect(Collectors.groupingBy(
                        InstrumentStatusDistributionProjection::getLimitStatus,
                        Collectors.summingLong(InstrumentStatusDistributionProjection::getTotal)));

        long totalWithStatus = overallStatusMap.values().stream()
                .mapToLong(Long::longValue).sum();

        List<CategoryCountDTO> overallStatusSummary
                = buildStatusCategories(overallStatusMap, totalWithStatus);

        List<InstrumentTypeDashboardDTO> byType = typeCounts.stream()
                .map(tc -> {
                    Map<String, Long> typeStatusMap = statusByType
                            .getOrDefault(tc.getTypeId(), List.of()).stream()
                            .collect(Collectors.toMap(
                                    InstrumentStatusDistributionProjection::getLimitStatus,
                                    InstrumentStatusDistributionProjection::getTotal));

                    long typeStatusTotal = typeStatusMap.values().stream()
                            .mapToLong(Long::longValue).sum();

                    return new InstrumentTypeDashboardDTO(
                            tc.getTypeId(),
                            tc.getTypeName(),
                            tc.getTotal(),
                            buildStatusCategories(typeStatusMap, typeStatusTotal));
                })
                .toList();

        return new InstrumentDashboardSummaryDTO(
                totalInstruments, totalTypes, overallStatusSummary, byType);
    }

    private List<CategoryCountDTO> buildStatusCategories(Map<String, Long> statusMap, long total) {
        return STATUS_ORDER.stream()
                .map(status -> {
                    long count = statusMap.getOrDefault(status, 0L);
                    double percentage = total > 0 ? Math.round(count * 1000.0 / total) / 10.0 : 0.0;
                    return new CategoryCountDTO(status, count, percentage);
                })
                .toList();
    }
}
