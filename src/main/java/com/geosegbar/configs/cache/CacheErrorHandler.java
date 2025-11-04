package com.geosegbar.configs.cache;

import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CacheErrorHandler implements org.springframework.cache.interceptor.CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("⚠️  Erro ao buscar cache: cache={}, key={}, error={}",
                cache.getName(), key, exception.getMessage());
        // ✅ Continua sem cache (aplicação não quebra)
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        // ⚠️ Verificar se é erro de read-only
        if (exception.getMessage() != null && exception.getMessage().contains("READONLY")) {
            log.error("❌ ERRO CRÍTICO: Redis está em modo READ-ONLY! cache={}, key={}",
                    cache.getName(), key);
            log.error("❌ AÇÃO NECESSÁRIA: Executar 'docker exec redis-prod redis-cli REPLICAOF NO ONE'");
        } else {
            log.error("❌ Erro ao escrever cache: cache={}, key={}, error={}",
                    cache.getName(), key, exception.getMessage());
        }
        // ✅ NÃO lança exceção - aplicação continua funcionando sem cache
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("⚠️  Erro ao invalidar cache: cache={}, key={}, error={}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("⚠️  Erro ao limpar cache: cache={}, error={}",
                cache.getName(), exception.getMessage());
    }
}
