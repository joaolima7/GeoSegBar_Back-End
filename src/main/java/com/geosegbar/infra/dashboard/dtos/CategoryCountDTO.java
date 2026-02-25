package com.geosegbar.infra.dashboard.dtos;

public record CategoryCountDTO(
        String name,
        long count,
        double percentage
        ) {

}
