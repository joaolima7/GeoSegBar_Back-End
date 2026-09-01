package com.geosegbar.infra.anomaly.dtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Anomalia numa listagem, com os campos escalares e nada de grafo.
 *
 * As rotas antigas devolvem a AnomalyEntity inteira, que arrasta o usuário
 * (com seus clientes), a barragem (com cliente, seções, instrumentos) e as
 * fotos. Aqui vai só o que uma lista precisa mostrar.
 *
 * Há um motivo técnico além do tamanho: com spring.jpa.open-in-view=false, a
 * sessão fecha antes de o Jackson serializar, e qualquer associação lazy —
 * photos, no caso — estoura LazyInitializationException. Montar o DTO dentro
 * da transação resolve isso de forma definitiva, em vez de depender de o
 * @EntityGraph cobrir todos os caminhos que o serializador vai percorrer.
 */
public record AnomalyListItemDTO(
        Long id,
        LocalDateTime createdAt,
        Long damId,
        String damName,
        Long userId,
        String userName,
        Double latitude,
        Double longitude,
        String origin,
        String observation,
        String recommendation,
        Long dangerLevelId,
        String dangerLevelName,
        Long statusId,
        String statusName,
        Long questionnaireId,
        Long questionId,
        List<String> photoPaths) {

}
