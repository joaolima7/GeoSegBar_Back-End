package com.geosegbar.configs.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class CustomMetrics implements MeterBinder {

    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong totalLogins = new AtomicLong(0);

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {

        Gauge.builder("app.users.active", activeUsers, AtomicLong::get)
                .description("Número de usuários ativos no momento")
                .register(registry);

        Counter.builder("app.logins.total")
                .description("Total de logins realizados")
                .register(registry);

        log.info("✅ Métricas customizadas registradas no Micrometer");
    }

    public void incrementActiveUsers() {
        activeUsers.incrementAndGet();
    }

    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }

    public void incrementTotalLogins() {
        totalLogins.incrementAndGet();
    }
}
