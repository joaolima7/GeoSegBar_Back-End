package com.geosegbar.infra.dashboard.dtos;

public record DamResponseSummaryDTO(
        Long damId,
        String damName,
        long totalResponses) {

}
