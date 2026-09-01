package com.geosegbar.infra.anomaly.dtos;

import java.util.List;

/**
 * Página de anomalias, no mesmo formato que o projeto já usa para respostas de
 * checklist — para o consumidor não ter que aprender uma segunda convenção de
 * paginação.
 */
public record PagedAnomalyDTO<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last,
        boolean first) {

}
