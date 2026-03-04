package com.geosegbar.infra.dashboard.dtos;

import java.util.List;

public record InstrumentTypeDashboardDTO(
        Long instrumentTypeId,
        String instrumentTypeName,
        long totalInstruments,
        List<CategoryCountDTO> statusDistribution) {

}
