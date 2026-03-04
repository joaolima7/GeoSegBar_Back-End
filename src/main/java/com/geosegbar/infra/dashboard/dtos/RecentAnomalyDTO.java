package com.geosegbar.infra.dashboard.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record RecentAnomalyDTO(
        Long id,
        Long userId,
        String userName,
        Long damId,
        String damName,
        LocalDateTime createdAt,
        Double latitude,
        Double longitude,
        String origin,
        String observation,
        String recommendation,
        String dangerLevelName,
        String statusName,
        List<String> photoUrls) {

}
