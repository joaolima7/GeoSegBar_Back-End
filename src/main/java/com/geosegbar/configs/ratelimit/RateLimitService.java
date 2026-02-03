package com.geosegbar.configs.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.redis.core.StringRedisTemplate;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RateLimitProperties properties;
    private final StringRedisTemplate redisTemplate;

    private final Map<String, ProxyManager<String>> proxyManagerCache = new ConcurrentHashMap<>();

    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong blockedRequests = new AtomicLong(0);

    @PostConstruct
    public void init() {
        if (properties.isEnabled()) {
            log.info("╔════════════════════════════════════════════════════════════════╗");
            log.info("║          RATE LIMITING ATIVADO - CONFIGURAÇÃO                  ║");
            log.info("╠════════════════════════════════════════════════════════════════╣");
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
        } else {
            log.warn("⚠️  RATE LIMITING DESABILITADO - Todas as requisições serão permitidas");
        }
    }

    public RateLimitInfo tryConsume(String identifier, RateLimitType type) {
        if (!properties.isEnabled()) {
            // Rate limiting desabilitado - permitir todas requisições
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

                log.info("✅ Rate limit OK - Type: {}, ID: {}, Remaining: {}/{}, Total allowed: {}",
                        type, identifier, remaining, config.getCapacity(), allowedRequests.get());

                return new RateLimitInfo(true, remaining, secondsUntilRefill, config.getCapacity());
            } else {
                blockedRequests.incrementAndGet();
                long nanosUntilRefill = probe.getNanosToWaitForReset();
                long secondsUntilRefill = Duration.ofNanos(nanosUntilRefill).getSeconds();

                log.warn("🚫 Rate limit BLOCKED - Type: {}, ID: {}, Retry in: {}s, Total blocked: {}",
                        identifier, type, secondsUntilRefill, blockedRequests.get());

                RateLimitProperties.LimitConfig config = properties.getConfigForType(type);
                return new RateLimitInfo(false, 0, secondsUntilRefill, config.getCapacity());
            }
        } catch (Exception e) {
            log.error("Erro ao processar rate limit para identifier: {}, type: {}", identifier, type, e);
            // Em caso de erro, permitir requisição (fail-open)
            return new RateLimitInfo(true, Long.MAX_VALUE, 0, Long.MAX_VALUE);
        }
    }

    private Bucket resolveBucket(String identifier, RateLimitType type) {
        String bucketKey = buildBucketKey(identifier, type);
        ProxyManager<String> proxyManager = getOrCreateProxyManager();

        BucketConfiguration configuration = buildBucketConfiguration(type);

        return proxyManager.builder().build(bucketKey, configuration);
    }

    private String buildBucketKey(String identifier, RateLimitType type) {
        return String.format("rate-limit:%s:%s", type.name().toLowerCase(), identifier);
    }

    private BucketConfiguration buildBucketConfiguration(RateLimitType type) {
        RateLimitProperties.LimitConfig config = properties.getConfigForType(type);

        Refill refill = Refill.intervally(
                config.getRefillTokens(),
                Duration.ofMinutes(config.getRefillDurationMinutes())
        );

        Bandwidth bandwidth = Bandwidth.classic(config.getCapacity(), refill);

        return BucketConfiguration.builder()
                .addLimit(bandwidth)
                .build();
    }

    private ProxyManager<String> getOrCreateProxyManager() {
        return proxyManagerCache.computeIfAbsent("default", key -> {
            try {
                String redisUri = String.format("redis://%s:%s",
                        redisTemplate.getConnectionFactory().getConnection().getConfig("bind").getProperty("bind"),
                        redisTemplate.getConnectionFactory().getConnection().getConfig("port").getProperty("port")
                );

                RedisClient redisClient = RedisClient.create(redisUri);
                StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                        RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
                );

                return LettuceBasedProxyManager.builderFor(connection)
                        .build();
            } catch (Exception e) {
                log.error("Erro ao criar ProxyManager para Redis", e);
                throw new RuntimeException("Falha ao inicializar rate limiting com Redis", e);
            }
        });
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

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
