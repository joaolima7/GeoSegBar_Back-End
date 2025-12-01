package com.geosegbar.configs.observability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@Endpoint(id = "performance")
@RequiredArgsConstructor
public class PerformanceDiagnosticsEndpoint {

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @ReadOperation
    public Map<String, Object> performance() {
        Map<String, Object> diagnostics = new HashMap<>();

        // ⭐ Top 10 métodos mais lentos
        diagnostics.put("slowest_methods", getTop10SlowestMethods());

        // ⭐ Estatísticas do HikariCP
        diagnostics.put("hikaricp_stats", getHikariStats());

        // ⭐ Estatísticas de cache
        diagnostics.put("cache_stats", getCacheStats());

        // ⭐ Redis info
        diagnostics.put("redis_stats", getRedisStats());

        // ⭐ Queries mais executadas
        diagnostics.put("query_stats", getQueryStats());

        return diagnostics;
    }

    private List<Map<String, Object>> getTop10SlowestMethods() {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().startsWith("service."))
                .filter(meter -> meter instanceof Timer)
                .map(meter -> {
                    Timer timer = (Timer) meter;
                    Map<String, Object> methodStats = new HashMap<>();
                    methodStats.put("method", meter.getId().getName());
                    methodStats.put("count", timer.count());
                    methodStats.put("mean_ms", timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                    methodStats.put("max_ms", timer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
                    methodStats.put("p95_ms", timer.percentile(0.95, java.util.concurrent.TimeUnit.MILLISECONDS));
                    methodStats.put("p99_ms", timer.percentile(0.99, java.util.concurrent.TimeUnit.MILLISECONDS));
                    return methodStats;
                })
                .sorted((a, b) -> Double.compare(
                (Double) b.get("mean_ms"),
                (Double) a.get("mean_ms")))
                .limit(10)
                .collect(Collectors.toList());
    }

    private Map<String, Object> getHikariStats() {
        Map<String, Object> stats = new HashMap<>();
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();

            stats.put("active_connections", pool.getActiveConnections());
            stats.put("idle_connections", pool.getIdleConnections());
            stats.put("total_connections", pool.getTotalConnections());
            stats.put("threads_awaiting_connection", pool.getThreadsAwaitingConnection());
            stats.put("max_pool_size", hikari.getMaximumPoolSize());
        }
        return stats;
    }

    private Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        cacheManager.getCacheNames().forEach(cacheName -> {
            Map<String, Object> cacheInfo = new HashMap<>();
            cacheInfo.put("name", cacheName);
            // Adicionar estatísticas básicas
            stats.put(cacheName, cacheInfo);
        });
        return stats;
    }

    private Map<String, Object> getRedisStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // ⭐ CORRIGIDO: Usar RedisCallback explícito
            Properties info = redisTemplate.execute((RedisCallback<Properties>) RedisConnection::info);
            stats.put("connected", true);
            stats.put("info", info != null ? info.toString() : "N/A");
        } catch (Exception e) {
            stats.put("connected", false);
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    private Map<String, Object> getQueryStats() {
        Map<String, Object> stats = new HashMap<>();

        meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals("hibernate.query.execution"))
                .forEach(meter -> {
                    Timer timer = (Timer) meter;
                    String queryType = meter.getId().getTag("query_type");

                    Map<String, Object> queryInfo = new HashMap<>();
                    queryInfo.put("count", timer.count());
                    queryInfo.put("mean_ms", timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                    queryInfo.put("max_ms", timer.max(java.util.concurrent.TimeUnit.MILLISECONDS));

                    stats.put(queryType != null ? queryType : "UNKNOWN", queryInfo);
                });

        return stats;
    }
}
