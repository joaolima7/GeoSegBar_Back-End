package com.geosegbar.configs.ratelimit;

/**
 * Tipos de rate limiting aplicados na aplicação.
 */
public enum RateLimitType {
    /**
     * Rate limiting para endpoints públicos (não autenticados). Identificação
     * por endereço IP.
     */
    PUBLIC,
    /**
     * Rate limiting para endpoints autenticados. Identificação por ID do
     * usuário.
     */
    AUTHENTICATED
}
