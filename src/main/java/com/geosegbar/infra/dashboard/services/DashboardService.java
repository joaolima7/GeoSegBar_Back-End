package com.geosegbar.infra.dashboard.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.dashboard.dtos.CategoryCountDTO;
import com.geosegbar.infra.dashboard.dtos.DashboardCategorySummaryDTO;
import com.geosegbar.infra.dashboard.projections.CategoryCountProjection;
import com.geosegbar.infra.permissions.dam_permissions.persistence.DamPermissionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AnomalyRepository anomalyRepository;
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

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard-danger-level-summary",
            key = "#damIds.toString() + ':' + #startDate + ':' + #endDate")
    public DashboardCategorySummaryDTO getDangerLevelSummary(
            List<Long> damIds, LocalDate startDate, LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<CategoryCountProjection> counts
                = anomalyRepository.countByDangerLevelGrouped(damIds, start, end);

        return buildSummary(counts);
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

        return buildSummary(counts);
    }

    private DashboardCategorySummaryDTO buildSummary(List<CategoryCountProjection> counts) {
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
}
