package com.geosegbar.infra.dashboard.dtos;

public record ChecklistResponseSummaryDTO(
        Long checklistId,
        String checklistName,
        long totalResponses) {

}
