package com.geosegbar.infra.dashboard.dtos;

import java.util.List;

public record InstrumentDashboardSummaryDTO(
        long totalInstruments,
        int totalInstrumentTypes,
        List<CategoryCountDTO> overallStatusSummary,
        List<InstrumentTypeDashboardDTO> byInstrumentType) {

}
