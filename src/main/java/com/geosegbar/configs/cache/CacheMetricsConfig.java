package com.geosegbar.configs.cache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.github.benmanes.caffeine.cache.stats.CacheStats;

import lombok.extern.slf4j.Slf4j; // Import correto para o log

@Configuration
@Slf4j // Anotação para usar o log
public class CacheMetricsConfig {

    @EventListener
    public void logCacheStatistics(ContextRefreshedEvent event) {
        try {
            CacheManager cacheManager = event.getApplicationContext()
                    .getBean("instrumentGraphCacheManager", CacheManager.class);

            // Log estatísticas periodicamente
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(() -> {
                try {
                    cacheManager.getCacheNames().forEach(cacheName -> {
                        Cache cache = cacheManager.getCache(cacheName);
                        if (cache instanceof CaffeineCache) {
                            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache
                                    = ((CaffeineCache) cache).getNativeCache();
                            CacheStats stats = nativeCache.stats();

                            log.info("Cache {}: Hit Rate: {:.2f}%, Evictions: {}, Size: {}, Load Count: {}",
                                    cacheName,
                                    stats.hitRate() * 100,
                                    stats.evictionCount(),
                                    nativeCache.estimatedSize(),
                                    stats.loadCount());
                        }
                    });
                } catch (Exception e) {
                    log.error("Erro ao coletar estatísticas de cache", e);
                }
            }, 5, 5, TimeUnit.MINUTES);

            log.info("Cache metrics configurado com sucesso - relatórios a cada 5 minutos");
        } catch (Exception e) {
            log.warn("Cache manager não encontrado ou erro na configuração de métricas", e);
        }
    }
}
