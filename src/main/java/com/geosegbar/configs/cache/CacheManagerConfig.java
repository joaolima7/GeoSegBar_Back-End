package com.geosegbar.configs.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableCaching
public class CacheManagerConfig implements CachingConfigurer {

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(1);

    private final CacheErrorHandler cacheErrorHandler;

    public CacheManagerConfig(CacheErrorHandler cacheErrorHandler) {
        this.cacheErrorHandler = cacheErrorHandler;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler;
    }

    private ObjectMapper redisObjectMapper() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
                .builder()
                .allowIfBaseType(Object.class)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new Hibernate5JakartaModule());

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        return mapper;
    }

    private RedisCacheConfiguration createRedisCacheConfiguration(Duration ttl) {

        GenericJackson2JsonRedisSerializer serializer
                = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    private RedisCacheManager createRedisCacheManager(
            RedisConnectionFactory connectionFactory,
            Map<String, Duration> ttlByCache,
            Duration defaultTtl) {

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        for (Map.Entry<String, Duration> entry : ttlByCache.entrySet()) {
            cacheConfigurations.put(
                    entry.getKey(),
                    createRedisCacheConfiguration(entry.getValue())
            );
        }

        RedisCacheConfiguration defaultConfig = createRedisCacheConfiguration(defaultTtl);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean("instrumentGraphCacheManager")
    @Primary
    public CacheManager instrumentGraphCacheManager(RedisConnectionFactory connectionFactory) {

        Map<String, Duration> ttlByCache = new HashMap<>();
        ttlByCache.put("graphPatternsByDam", DEFAULT_CACHE_TTL);
        ttlByCache.put("graphPatternById", DEFAULT_CACHE_TTL);
        ttlByCache.put("graphPatternsByInstrument", DEFAULT_CACHE_TTL);
        ttlByCache.put("folderWithPatterns", DEFAULT_CACHE_TTL);
        ttlByCache.put("damFoldersWithPatterns", DEFAULT_CACHE_TTL);
        ttlByCache.put("graphProperties", DEFAULT_CACHE_TTL);
        ttlByCache.put("graphAxes", DEFAULT_CACHE_TTL);

        return createRedisCacheManager(connectionFactory, ttlByCache, DEFAULT_CACHE_TTL);
    }

    @Bean("instrumentTabulateCacheManager")
    public CacheManager instrumentTabulateCacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, Duration> ttlByCache = new HashMap<>();
        ttlByCache.put("tabulatePatterns", DEFAULT_CACHE_TTL);
        ttlByCache.put("tabulatePatternsByDam", DEFAULT_CACHE_TTL);
        ttlByCache.put("tabulatePatternsByFolder", DEFAULT_CACHE_TTL);
        ttlByCache.put("tabulateFolderWithPatterns", DEFAULT_CACHE_TTL);
        ttlByCache.put("damTabulateFoldersWithPatterns", DEFAULT_CACHE_TTL);
        ttlByCache.put("tabulateFoldersByDam", DEFAULT_CACHE_TTL);

        return createRedisCacheManager(connectionFactory, ttlByCache, DEFAULT_CACHE_TTL);
    }

    @Bean("checklistCacheManager")
    public CacheManager checklistCacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, Duration> ttlByCache = new HashMap<>();
        ttlByCache.put("checklistsByDam", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistsWithAnswersByDam", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistsWithAnswersByClient", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistById", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistForDam", DEFAULT_CACHE_TTL);
        ttlByCache.put("allChecklists", DEFAULT_CACHE_TTL);
        ttlByCache.put("allChecklistResponses", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistResponseById", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistResponsesByDam", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistResponseDetail", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistResponsesByUser", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistResponsesByDate", DEFAULT_CACHE_TTL);
        ttlByCache.put("damLastChecklist", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistResponsesByDamPaged", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistResponsesByUserPaged", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistResponsesByDatePaged", DEFAULT_CACHE_TTL);
        ttlByCache.put("allChecklistResponsesPaged", DEFAULT_CACHE_TTL);
        ttlByCache.put("checklistResponsesByClient", DEFAULT_CACHE_TTL);
        ttlByCache.put("clientLatestDetailedChecklistResponses", DEFAULT_CACHE_TTL);

        return createRedisCacheManager(connectionFactory, ttlByCache, DEFAULT_CACHE_TTL);
    }

    @Bean("instrumentCacheManager")
    public CacheManager instrumentCacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, Duration> ttlByCache = new HashMap<>();
        ttlByCache.put("instrumentById", DEFAULT_CACHE_TTL);
        ttlByCache.put("instrumentWithDetails", DEFAULT_CACHE_TTL);
        ttlByCache.put("instrumentsByClient", DEFAULT_CACHE_TTL);
        ttlByCache.put("instrumentsByFilters", DEFAULT_CACHE_TTL);
        ttlByCache.put("instrumentsByDam", DEFAULT_CACHE_TTL);
        ttlByCache.put("allInstruments", DEFAULT_CACHE_TTL);
        ttlByCache.put("instrumentResponseDTO", DEFAULT_CACHE_TTL);

        return createRedisCacheManager(connectionFactory, ttlByCache, DEFAULT_CACHE_TTL);
    }

    @Bean("readingCacheManager")
    public CacheManager readingCacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, Duration> ttlByCache = new HashMap<>();
        ttlByCache.put("readingsByInstrument", DEFAULT_CACHE_TTL);
        ttlByCache.put("instrumentLimitStatus", DEFAULT_CACHE_TTL);
        ttlByCache.put("clientInstrumentLatestGroupedReadings", DEFAULT_CACHE_TTL);
        ttlByCache.put("groupedReadings", DEFAULT_CACHE_TTL);
        ttlByCache.put("readingsByFiltersOptimized", DEFAULT_CACHE_TTL);
        ttlByCache.put("multiInstrumentReadings", DEFAULT_CACHE_TTL);
        ttlByCache.put("clientInstrumentLimitStatuses", DEFAULT_CACHE_TTL);
        ttlByCache.put("readingById", DEFAULT_CACHE_TTL);
        ttlByCache.put("readingResponseDTO", DEFAULT_CACHE_TTL);
        ttlByCache.put("readingExists", DEFAULT_CACHE_TTL);
        ttlByCache.put("latestReadings", DEFAULT_CACHE_TTL);
        ttlByCache.put("readingsByOutput", DEFAULT_CACHE_TTL);
        ttlByCache.put("readingsByFilters", DEFAULT_CACHE_TTL);

        return createRedisCacheManager(connectionFactory, ttlByCache, DEFAULT_CACHE_TTL);
    }
}
