package com.geosegbar.configs.observability;

import java.util.concurrent.TimeUnit;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ Interceptor de queries SQL usando StatementInspector do Hibernate 6+
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueryPerformanceInterceptor implements StatementInspector {

    private final MeterRegistry meterRegistry;
    private final ThreadLocal<Long> queryStartTime = new ThreadLocal<>();

    @Override
    public String inspect(String sql) {
        // Marca início da query
        queryStartTime.set(System.nanoTime());

        // Registrar execução após retorno (usando aspect separado)
        registerQueryExecution(sql);

        return sql;
    }

    private void registerQueryExecution(String sql) {
        Long startTime = queryStartTime.get();
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);

            String queryType = detectQueryType(sql);

            // ⭐ Registrar métrica no Micrometer
            Timer.builder("hibernate.query.execution")
                    .tag("query_type", queryType)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.NANOSECONDS);

            // ⭐ Log de queries lentas
            if (durationMs > 100) {
                String queryPreview = sql.length() > 200 ? sql.substring(0, 197) + "..." : sql;
                log.warn("🐌 SLOW QUERY ({}ms) [{}]: {}", durationMs, queryType, queryPreview);

                meterRegistry.counter("slow.queries",
                        "query_type", queryType).increment();
            } else if (log.isDebugEnabled()) {
                log.debug("✅ QUERY ({}ms) [{}]", durationMs, queryType);
            }

            queryStartTime.remove();
        }
    }

    private String detectQueryType(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "UNKNOWN";
        }

        String upperSql = sql.trim().toUpperCase();

        if (upperSql.startsWith("SELECT")) {
            return "SELECT";
        }
        if (upperSql.startsWith("INSERT")) {
            return "INSERT";
        }
        if (upperSql.startsWith("UPDATE")) {
            return "UPDATE";
        }
        if (upperSql.startsWith("DELETE")) {
            return "DELETE";
        }
        if (upperSql.startsWith("WITH")) {
            return "CTE"; // Common Table Expression
        }
        return "OTHER";
    }
}
