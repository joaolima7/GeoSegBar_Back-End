package com.geosegbar.infra.dashboard.dtos;

import java.util.List;

public record ChecklistDashboardSummaryDTO(
        long totalChecklists,
        long totalResponses,
        long totalRespondents,
        List<ChecklistResponseSummaryDTO> responsesByChecklist,
        List<CategoryCountDTO> weatherConditionDistribution,
        List<DamResponseSummaryDTO> responsesByDam) {

}
