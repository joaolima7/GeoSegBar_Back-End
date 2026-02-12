package com.geosegbar.configs.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RateLimitProperties properties;

    // Injetamos as propriedades do Redis diretamente do Spring Boot
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}") // Padrão vazio se não tiver senha
    private String redisPassword;

    private final Map<String, ProxyManager<String>> proxyManagerCache = new ConcurrentHashMap<>();
    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong blockedRequests = new AtomicLong(0);

    private RedisClient redisClient;
    private StatefulRedisConnection<String, byte[]> redisConnection;

    @PostConstruct
    public void init() {
        if (properties.isEnabled()) {
            log.info("╔════════════════════════════════════════════════════════════════╗");
            log.info("║          RATE LIMITING ATIVADO - CONFIGURAÇÃO                  ║");
            log.info("╠════════════════════════════════════════════════════════════════╣");
            log.info("║ Redis Host : {}:{}                                         ║", redisHost, redisPort);
            log.info("║ PUBLIC     - Capacity: {} req/{}min | Refill: {} tokens/{}min  ║",
                    String.format("%3d", properties.getPublicConfig().getCapacity()),
                    properties.getPublicConfig().getRefillDurationMinutes(),
                    String.format("%3d", properties.getPublicConfig().getRefillTokens()),
                    properties.getPublicConfig().getRefillDurationMinutes());
            log.info("║ AUTHENTICATED - Capacity: {} req/{}min | Refill: {} tokens/{}min ║",
                    String.format("%3d", properties.getAuthenticated().getCapacity()),
                    properties.getAuthenticated().getRefillDurationMinutes(),
                    String.format("%3d", properties.getAuthenticated().getRefillTokens()),
                    properties.getAuthenticated().getRefillDurationMinutes());
            log.info("╚════════════════════════════════════════════════════════════════╝");

            // Inicializa a conexão Redis dedicada ao Bucket4j
            initRedisConnection();
        } else {
            log.warn("⚠️  RATE LIMITING DESABILITADO - Todas as requisições serão permitidas");
        }
    }

    private void initRedisConnection() {
        try {
            // Monta a URL correta: redis://[password@]host:port
            StringBuilder redisUri = new StringBuilder("redis://");
            if (redisPassword != null && !redisPassword.isBlank()) {
                redisUri.append(redisPassword).append("@");
            }
            redisUri.append(redisHost).append(":").append(redisPort);

            this.redisClient = RedisClient.create(redisUri.toString());
            this.redisConnection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

            log.info("✅ Conexão Redis para Rate Limit estabelecida com sucesso!");
        } catch (Exception e) {
            log.error("❌ Falha crítica ao conectar no Redis para Rate Limit: {}", e.getMessage());
            // Não lançamos exceção aqui para não impedir o boot da aplicação, 
            // mas o rate limit não funcionará (fail-open no método tryConsume)
        }
    }

    @PreDestroy
    public void shutdown() {
        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    public RateLimitInfo tryConsume(String identifier, RateLimitType type) {
        if (!properties.isEnabled() || redisConnection == null || !redisConnection.isOpen()) {
            // Fail-open: Se desabilitado ou Redis fora do ar, permite tudo
            return new RateLimitInfo(true, Long.MAX_VALUE, 0, Long.MAX_VALUE);
        }

        try {
            Bucket bucket = resolveBucket(identifier, type);
            var probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                allowedRequests.incrementAndGet();
                long remaining = probe.getRemainingTokens();
                long nanosUntilRefill = probe.getNanosToWaitForRefill();
                long secondsUntilRefill = Duration.ofNanos(nanosUntilRefill).getSeconds();
                RateLimitProperties.LimitConfig config = properties.getConfigForType(type);

                return new RateLimitInfo(true, remaining, secondsUntilRefill, config.getCapacity());
            } else {
                blockedRequests.incrementAndGet();
                long nanosUntilRefill = probe.getNanosToWaitForReset();
                long secondsUntilRefill = Duration.ofNanos(nanosUntilRefill).getSeconds();
                RateLimitProperties.LimitConfig config = properties.getConfigForType(type);

                log.warn("🚫 Rate limit BLOCKED - Type: {}, ID: {}, Retry in: {}s", identifier, type, secondsUntilRefill);
                return new RateLimitInfo(false, 0, secondsUntilRefill, config.getCapacity());
            }
        } catch (Exception e) {
            log.error("Erro ao processar rate limit para identifier: {}", identifier, e);
            // Fail-open em caso de erro no Redis durante a execução
            return new RateLimitInfo(true, Long.MAX_VALUE, 0, Long.MAX_VALUE);
        }
    }

    private Bucket resolveBucket(String identifier, RateLimitType type) {
        String bucketKey = buildBucketKey(identifier, type);
        ProxyManager<String> proxyManager = getProxyManager();
        BucketConfiguration configuration = buildBucketConfiguration(type);
        return proxyManager.builder().build(bucketKey, configuration);
    }

    private String buildBucketKey(String identifier, RateLimitType type) {
        return String.format("rate-limit:%s:%s", type.name().toLowerCase(), identifier);
    }

    private BucketConfiguration buildBucketConfiguration(RateLimitType type) {
        RateLimitProperties.LimitConfig config = properties.getConfigForType(type);
        Refill refill = Refill.intervally(config.getRefillTokens(), Duration.ofMinutes(config.getRefillDurationMinutes()));
        Bandwidth bandwidth = Bandwidth.classic(config.getCapacity(), refill);
        return BucketConfiguration.builder().addLimit(bandwidth).build();
    }

    // Singleton do ProxyManager usando a conexão criada no init()
    private ProxyManager<String> getProxyManager() {
        return proxyManagerCache.computeIfAbsent("default", key
                -> LettuceBasedProxyManager.builderFor(redisConnection).build()
        );
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    // ... getStatistics mantido igual ...
    public Map<String, Object> getStatistics() {
        return Map.of(
                "enabled", properties.isEnabled(),
                "allowedRequests", allowedRequests.get(),
                "blockedRequests", blockedRequests.get(),
                "totalRequests", allowedRequests.get() + blockedRequests.get(),
                "blockRate", allowedRequests.get() + blockedRequests.get() > 0
                ? String.format("%.2f%%", (blockedRequests.get() * 100.0) / (allowedRequests.get() + blockedRequests.get()))
                : "0.00%",
                "publicLimit", properties.getPublicConfig().getCapacity() + " req/" + properties.getPublicConfig().getRefillDurationMinutes() + "min",
                "authenticatedLimit", properties.getAuthenticated().getCapacity() + " req/" + properties.getAuthenticated().getRefillDurationMinutes() + "min"
        );
    }
}
