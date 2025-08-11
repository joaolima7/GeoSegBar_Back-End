package com.geosegbar.configs.cache;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableCaching
@Slf4j
public class CacheManagerConfig {

    @Bean("instrumentGraphCacheManager")
    public CacheManager instrumentGraphCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                // Cache para consultas por barragem (dados grandes, menos frequentes)
                new CaffeineCache("graphPatternsByDam",
                        Caffeine.newBuilder()
                                .maximumSize(50) // Reduzido - dados grandes por barragem
                                .expireAfterWrite(Duration.ofMinutes(20)) // TTL maior para dados estáveis
                                .expireAfterAccess(Duration.ofMinutes(10)) // Remove se não acessado
                                .recordStats()
                                .removalListener((key, value, cause)
                                        -> log.debug("Cache eviction - graphPatternsByDam: key={}, cause={}", key, cause))
                                .build()),
                // Cache para padrões individuais (acesso frequente)
                new CaffeineCache("graphPatternById",
                        Caffeine.newBuilder()
                                .maximumSize(300) // Aumentado para padrões individuais
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .recordStats()
                                .build()),
                // Cache para consultas por instrumento
                new CaffeineCache("graphPatternsByInstrument",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .recordStats()
                                .build()),
                // Cache para pastas com padrões (dados médios)
                new CaffeineCache("folderWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(80) // Ajustado para uso real
                                .expireAfterWrite(Duration.ofMinutes(18))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .recordStats()
                                .build()),
                // Cache para pastas por barragem (dados grandes, menos frequentes)
                new CaffeineCache("damFoldersWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(25) // Limitado - dados muito grandes
                                .expireAfterWrite(Duration.ofMinutes(25))
                                .expireAfterAccess(Duration.ofMinutes(12))
                                .recordStats()
                                .removalListener((key, value, cause)
                                        -> log.debug("Cache eviction - damFoldersWithPatterns: key={}, cause={}", key, cause))
                                .build()),
                // Cache para propriedades (acesso muito frequente)
                new CaffeineCache("graphProperties",
                        Caffeine.newBuilder()
                                .maximumSize(400) // Aumentado - dados pequenos, acesso frequente
                                .expireAfterWrite(Duration.ofMinutes(12))
                                .expireAfterAccess(Duration.ofMinutes(6))
                                .recordStats()
                                .build()),
                // Cache para eixos (dados pequenos, estáveis)
                new CaffeineCache("graphAxes",
                        Caffeine.newBuilder()
                                .maximumSize(200)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .recordStats()
                                .build())
        ));

        log.info("Cache manager 'instrumentGraphCacheManager' configurado com {} caches",
                cacheManager.getCacheNames().size());

        return cacheManager;
    }
}
