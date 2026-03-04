package com.geosegbar.infra.dashboard.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.WeatherConditionEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.anomaly_photo.persistence.jpa.AnomalyPhotoRepository;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.dashboard.dtos.CategoryCountDTO;
import com.geosegbar.infra.dashboard.dtos.ChecklistDashboardSummaryDTO;
import com.geosegbar.infra.dashboard.dtos.ChecklistResponseSummaryDTO;
import com.geosegbar.infra.dashboard.dtos.DamResponseSummaryDTO;
import com.geosegbar.infra.dashboard.dtos.DashboardCategorySummaryDTO;
import com.geosegbar.infra.dashboard.dtos.InstrumentDashboardSummaryDTO;
import com.geosegbar.infra.dashboard.dtos.InstrumentTypeDashboardDTO;
import com.geosegbar.infra.dashboard.dtos.RecentAnomalyDTO;
import com.geosegbar.infra.dashboard.projections.AnomalyPhotoPathProjection;
import com.geosegbar.infra.dashboard.projections.CategoryCountProjection;
import com.geosegbar.infra.dashboard.projections.InstrumentStatusDistributionProjection;
import com.geosegbar.infra.dashboard.projections.InstrumentTypeCountProjection;
import com.geosegbar.infra.dashboard.projections.RecentAnomalyProjection;
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
    private final AnomalyPhotoRepository anomalyPhotoRepository;
    private final InstrumentRepository instrumentRepository;
    private final ReadingRepository readingRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistResponseRepository checklistResponseRepository;
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

    // ======================== RECENT ANOMALIES ENDPOINT ========================
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard-recent-anomalies",
            key = "#damIds.toString() + ':' + #limit")
    public List<RecentAnomalyDTO> getRecentAnomalies(List<Long> damIds, int limit) {

        List<RecentAnomalyProjection> anomalies
                = anomalyRepository.findRecentByDamIds(damIds, limit);

        if (anomalies.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> anomalyIds = anomalies.stream()
                .map(RecentAnomalyProjection::getId)
                .toList();

        Map<Long, List<String>> photosByAnomalyId
                = anomalyPhotoRepository.findPathsByAnomalyIds(anomalyIds).stream()
                        .collect(Collectors.groupingBy(
                                AnomalyPhotoPathProjection::getAnomalyId,
                                Collectors.mapping(
                                        AnomalyPhotoPathProjection::getImagePath,
                                        Collectors.toList())));

        return anomalies.stream()
                .map(a -> new RecentAnomalyDTO(
                a.getId(),
                a.getUserId(),
                a.getUserName(),
                a.getDamId(),
                a.getDamName(),
                a.getCreatedAt(),
                a.getLatitude(),
                a.getLongitude(),
                a.getOrigin(),
                a.getObservation(),
                a.getRecommendation(),
                a.getDangerLevelName(),
                a.getStatusName(),
                photosByAnomalyId.getOrDefault(a.getId(), Collections.emptyList())))
                .toList();
    }

    // ======================== CHECKLIST ENDPOINT ========================
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard-checklist-summary",
            key = "#damIds.toString() + ':' + #startDate + ':' + #endDate")
    public ChecklistDashboardSummaryDTO getChecklistSummary(
            List<Long> damIds, LocalDate startDate, LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        long totalChecklists = checklistRepository.countByDamIds(damIds);

        long totalResponses = checklistResponseRepository
                .countByDamIdsAndDateRange(damIds, start, end);

        long totalRespondents = checklistResponseRepository
                .countDistinctRespondents(damIds, start, end);

        List<ChecklistResponseSummaryDTO> responsesByChecklist
                = checklistResponseRepository.countByChecklistGrouped(damIds, start, end)
                        .stream()
                        .map(p -> new ChecklistResponseSummaryDTO(
                        p.getChecklistId(), p.getChecklistName(), p.getTotal()))
                        .toList();

        List<CategoryCountDTO> weatherDistribution
                = buildWeatherDistribution(
                        checklistResponseRepository.countByWeatherConditionGrouped(damIds, start, end));

        List<DamResponseSummaryDTO> responsesByDam
                = checklistResponseRepository.countByDamGrouped(damIds, start, end)
                        .stream()
                        .map(p -> new DamResponseSummaryDTO(
                        p.getDamId(), p.getDamName(), p.getTotal()))
                        .toList();

        return new ChecklistDashboardSummaryDTO(
                totalChecklists, totalResponses, totalRespondents,
                responsesByChecklist, weatherDistribution, responsesByDam);
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

    private List<CategoryCountDTO> buildWeatherDistribution(List<CategoryCountProjection> counts) {
        Map<String, Long> weatherMap = counts.stream()
                .collect(Collectors.toMap(CategoryCountProjection::getName, CategoryCountProjection::getCount));

        long total = weatherMap.values().stream().mapToLong(Long::longValue).sum();

        return Arrays.stream(WeatherConditionEnum.values())
                .map(wc -> {
                    long count = weatherMap.getOrDefault(wc.name(), 0L);
                    double percentage = total > 0 ? Math.round(count * 1000.0 / total) / 10.0 : 0.0;
                    return new CategoryCountDTO(wc.name(), count, percentage);
                })
                .toList();
    }
}
