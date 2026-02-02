package com.geosegbar.configs.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Informações sobre o rate limit após tentativa de consumo.
 */
@Getter
@AllArgsConstructor
public class RateLimitInfo {

    /**
     * Se a requisição foi permitida (token consumido com sucesso).
     */
    private final boolean allowed;

    /**
     * Número de tokens restantes no bucket.
     */
    private final long remainingTokens;

    /**
     * Tempo em segundos até o próximo refill.
     */
    private final long secondsUntilRefill;

    /**
     * Capacidade total do bucket.
     */
    private final long capacity;
}
