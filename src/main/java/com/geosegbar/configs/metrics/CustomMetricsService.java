package com.geosegbar.configs.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomMetricsService {

    private final MeterRegistry meterRegistry;

    private final Counter readingsCreatedCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter authenticationErrorCounter;
    private final Counter checklistsSubmittedCounter;
    private final Counter questionnairesAnsweredCounter;

    private final Timer readingCreationTimer;
    private final Timer databaseQueryTimer;
    private final Timer cacheOperationTimer;

    private final AtomicLong activeInstruments = new AtomicLong(0);
    private final AtomicLong totalReadingsToday = new AtomicLong(0);
    private final AtomicLong activeUsers = new AtomicLong(0);

    private final Map<Long, Counter> readingsByInstrument = new ConcurrentHashMap<>();

    public CustomMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.readingsCreatedCounter = Counter.builder("geosegbar.readings.created.total")
                .description("Total de leituras criadas")
                .tag("service", "readings")
                .register(meterRegistry);

        this.cacheHitCounter = Counter.builder("geosegbar.cache.hit.total")
                .description("Total de cache hits")
                .tag("type", "redis")
                .register(meterRegistry);

        this.cacheMissCounter = Counter.builder("geosegbar.cache.miss.total")
                .description("Total de cache misses")
                .tag("type", "redis")
                .register(meterRegistry);

        this.loginSuccessCounter = Counter.builder("geosegbar.auth.login.success.total")
                .description("Total de logins bem-sucedidos")
                .tag("service", "auth")
                .register(meterRegistry);

        this.loginFailureCounter = Counter.builder("geosegbar.auth.login.failure.total")
                .description("Total de logins falhados")
                .tag("service", "auth")
                .register(meterRegistry);

        this.authenticationErrorCounter = Counter.builder("geosegbar.auth.error.total")
                .description("Total de erros de autenticação")
                .tag("service", "auth")
                .register(meterRegistry);

        this.checklistsSubmittedCounter = Counter.builder("geosegbar.checklists.submitted.total")
                .description("Total de checklists submetidos")
                .tag("service", "checklist")
                .register(meterRegistry);

        this.questionnairesAnsweredCounter = Counter.builder("geosegbar.questionnaires.answered.total")
                .description("Total de questionários respondidos")
                .tag("service", "checklist")
                .register(meterRegistry);

        this.readingCreationTimer = Timer.builder("geosegbar.readings.creation.time")
                .description("Tempo de criação de leituras")
                .tag("operation", "create")
                .register(meterRegistry);

        this.databaseQueryTimer = Timer.builder("geosegbar.database.query.time")
                .description("Tempo de queries no banco")
                .tag("operation", "query")
                .register(meterRegistry);

        this.cacheOperationTimer = Timer.builder("geosegbar.cache.operation.time")
                .description("Tempo de operações de cache")
                .tag("operation", "cache")
                .register(meterRegistry);

        Gauge.builder("geosegbar.instruments.active", activeInstruments, AtomicLong::get)
                .description("Número de instrumentos ativos")
                .tag("type", "gauge")
                .register(meterRegistry);

        Gauge.builder("geosegbar.readings.today", totalReadingsToday, AtomicLong::get)
                .description("Total de leituras criadas hoje")
                .tag("type", "gauge")
                .register(meterRegistry);

        Gauge.builder("geosegbar.users.active", activeUsers, AtomicLong::get)
                .description("Número de usuários ativos")
                .tag("type", "gauge")
                .register(meterRegistry);

        log.info("✅ CustomMetricsService inicializado com sucesso");
    }

    /**
     * Incrementa contador de leituras criadas
     */
    public void incrementReadingsCreated(long count) {
        readingsCreatedCounter.increment(count);
        totalReadingsToday.addAndGet(count);
        log.debug("📊 Readings created counter: +{}", count);
    }

    /**
     * Incrementa contador de leituras por instrumento específico
     */
    public void incrementReadingsForInstrument(Long instrumentId, long count) {
        Counter counter = readingsByInstrument.computeIfAbsent(instrumentId, id
                -> Counter.builder("geosegbar.readings.by_instrument.total")
                        .description("Leituras criadas por instrumento")
                        .tag("instrument_id", String.valueOf(id))
                        .register(meterRegistry)
        );
        counter.increment(count);
    }

    /**
     * Registra tempo de criação de leituras
     */
    public <T> T recordReadingCreation(java.util.function.Supplier<T> supplier) {
        return readingCreationTimer.record(supplier);
    }

    /**
     * Registra tempo de query no banco
     */
    public <T> T recordDatabaseQuery(java.util.function.Supplier<T> supplier) {
        return databaseQueryTimer.record(supplier);
    }

    /**
     * Registra tempo de operação de cache
     */
    public <T> T recordCacheOperation(java.util.function.Supplier<T> supplier) {
        return cacheOperationTimer.record(supplier);
    }

    /**
     * Incrementa cache hit
     */
    public void incrementCacheHit() {
        cacheHitCounter.increment();
    }

    /**
     * Incrementa cache miss
     */
    public void incrementCacheMiss() {
        cacheMissCounter.increment();
    }

    /**
     * Incrementa login bem-sucedido
     */
    public void incrementLoginSuccess() {
        loginSuccessCounter.increment();
        activeUsers.incrementAndGet();
    }

    /**
     * Incrementa login falhado
     */
    public void incrementLoginFailure() {
        loginFailureCounter.increment();
    }

    /**
     * Incrementa erro de autenticação
     */
    public void incrementAuthenticationError() {
        authenticationErrorCounter.increment();
    }

    /**
     * Decrementa usuários ativos (logout)
     */
    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }

    /**
     * Atualiza número de instrumentos ativos
     */
    public void setActiveInstruments(long count) {
        activeInstruments.set(count);
    }

    /**
     * Reseta contador de leituras do dia (executar à meia-noite)
     */
    public void resetDailyReadings() {
        totalReadingsToday.set(0);
        log.info("🔄 Daily readings counter resetado");
    }

    public void incrementChecklistsSubmitted() {
        checklistsSubmittedCounter.increment();
        log.debug("📊 Checklists submitted counter: +1");
    }

    /**
     * Incrementa contador de questionários respondidos
     */
    public void incrementQuestionnairesAnswered(long count) {
        questionnairesAnsweredCounter.increment(count);
        log.debug("📊 Questionnaires answered counter: +{}", count);
    }
}
