package com.geosegbar.infra.dashboard.dtos;

import java.util.List;

public record DashboardCategorySummaryDTO(
        long total,
        List<CategoryCountDTO> categories
        ) {

}
