package com.geosegbar.configs.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configurações de rate limiting carregadas do application.properties.
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    /**
     * Flag para habilitar/desabilitar rate limiting globalmente.
     */
    private boolean enabled = false;

    /**
     * Configurações para endpoints públicos (não autenticados).
     */
    private LimitConfig publicConfig = new LimitConfig();

    /**
     * Configurações para endpoints autenticados.
     */
    private LimitConfig authenticated = new LimitConfig();

    @Getter
    @Setter
    public static class LimitConfig {

        /**
         * Capacidade total do bucket (número máximo de tokens).
         */
        private long capacity = 100;

        /**
         * Número de tokens adicionados em cada ciclo de refill.
         */
        private long refillTokens = 100;

        /**
         * Duração do ciclo de refill em minutos.
         */
        private long refillDurationMinutes = 1;
    }

    /**
     * Retorna a configuração apropriada baseada no tipo de rate limit.
     */
    public LimitConfig getConfigForType(RateLimitType type) {
        return switch (type) {
            case PUBLIC ->
                publicConfig;
            case AUTHENTICATED ->
                authenticated;
        };
    }
}
