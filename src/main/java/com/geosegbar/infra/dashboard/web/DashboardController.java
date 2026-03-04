package com.geosegbar.infra.dashboard.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.dashboard.dtos.DashboardCategorySummaryDTO;
import com.geosegbar.infra.dashboard.dtos.InstrumentDashboardSummaryDTO;
import com.geosegbar.infra.dashboard.dtos.RecentAnomalyDTO;
import com.geosegbar.infra.dashboard.services.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final int DEFAULT_DAYS_RANGE = 60;
    private static final int DEFAULT_RECENT_LIMIT = 10;
    private static final int MAX_RECENT_LIMIT = 50;

    private final DashboardService dashboardService;

    @GetMapping("/anomalies/danger-level-summary")
    public ResponseEntity<WebResponseEntity<DashboardCategorySummaryDTO>> getDangerLevelSummary(
            @RequestParam List<Long> damIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Long> sortedDamIds = damIds.stream().sorted().toList();
        LocalDate resolvedStart = startDate != null ? startDate : LocalDate.now().minusDays(DEFAULT_DAYS_RANGE);
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();

        dashboardService.validateDamAccess(sortedDamIds);

        DashboardCategorySummaryDTO summary
                = dashboardService.getDangerLevelSummary(sortedDamIds, resolvedStart, resolvedEnd);

        return ResponseEntity.ok(
                WebResponseEntity.success(summary, "Resumo de níveis de perigo obtido com sucesso"));
    }

    @GetMapping("/anomalies/status-summary")
    public ResponseEntity<WebResponseEntity<DashboardCategorySummaryDTO>> getAnomalyStatusSummary(
            @RequestParam List<Long> damIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Long> sortedDamIds = damIds.stream().sorted().toList();
        LocalDate resolvedStart = startDate != null ? startDate : LocalDate.now().minusDays(DEFAULT_DAYS_RANGE);
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();

        dashboardService.validateDamAccess(sortedDamIds);

        DashboardCategorySummaryDTO summary
                = dashboardService.getAnomalyStatusSummary(sortedDamIds, resolvedStart, resolvedEnd);

        return ResponseEntity.ok(
                WebResponseEntity.success(summary, "Resumo de status de anomalias obtido com sucesso"));
    }

    @GetMapping("/instruments/summary")
    public ResponseEntity<WebResponseEntity<InstrumentDashboardSummaryDTO>> getInstrumentSummary(
            @RequestParam List<Long> damIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Long> sortedDamIds = damIds.stream().sorted().toList();
        LocalDate resolvedStart = startDate != null ? startDate : LocalDate.now().minusDays(DEFAULT_DAYS_RANGE);
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();

        dashboardService.validateDamAccess(sortedDamIds);

        InstrumentDashboardSummaryDTO summary
                = dashboardService.getInstrumentSummary(sortedDamIds, resolvedStart, resolvedEnd);

        return ResponseEntity.ok(
                WebResponseEntity.success(summary, "Resumo de instrumentos obtido com sucesso"));
    }

    @GetMapping("/anomalies/recent")
    public ResponseEntity<WebResponseEntity<List<RecentAnomalyDTO>>> getRecentAnomalies(
            @RequestParam List<Long> damIds,
            @RequestParam(required = false) Integer limit) {

        List<Long> sortedDamIds = damIds.stream().sorted().toList();
        int resolvedLimit = Math.min(
                limit != null && limit > 0 ? limit : DEFAULT_RECENT_LIMIT,
                MAX_RECENT_LIMIT);

        dashboardService.validateDamAccess(sortedDamIds);

        List<RecentAnomalyDTO> recent
                = dashboardService.getRecentAnomalies(sortedDamIds, resolvedLimit);

        return ResponseEntity.ok(
                WebResponseEntity.success(recent, "Anomalias recentes obtidas com sucesso"));
    }
}
