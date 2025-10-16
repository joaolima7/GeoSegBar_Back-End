package com.geosegbar.configs.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
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
public class CacheManagerConfig {

    private ObjectMapper redisObjectMapper() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
                .builder()
                .allowIfBaseType(Object.class)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Configurações específicas para Hibernate
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new Hibernate5JakartaModule());  // Adicionar módulo Hibernate

        // Outras configurações
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
        ttlByCache.put("graphPatternsByDam", Duration.ofMinutes(20));
        ttlByCache.put("graphPatternById", Duration.ofMinutes(15));
        ttlByCache.put("graphPatternsByInstrument", Duration.ofMinutes(15));
        ttlByCache.put("folderWithPatterns", Duration.ofMinutes(18));
        ttlByCache.put("damFoldersWithPatterns", Duration.ofMinutes(25));
        ttlByCache.put("graphProperties", Duration.ofMinutes(12));
        ttlByCache.put("graphAxes", Duration.ofMinutes(20));

        return createRedisCacheManager(connectionFactory, ttlByCache, Duration.ofMinutes(15));
    }

    @Bean("instrumentTabulateCacheManager")
    public CacheManager instrumentTabulateCacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, Duration> ttlByCache = new HashMap<>();
        ttlByCache.put("tabulatePatterns", Duration.ofMinutes(15));
        ttlByCache.put("tabulatePatternsByDam", Duration.ofMinutes(20));
        ttlByCache.put("tabulatePatternsByFolder", Duration.ofMinutes(15));
        ttlByCache.put("tabulateFolderWithPatterns", Duration.ofMinutes(18));
        ttlByCache.put("damTabulateFoldersWithPatterns", Duration.ofMinutes(25));

        return createRedisCacheManager(connectionFactory, ttlByCache, Duration.ofMinutes(15));
    }

    @Bean("checklistCacheManager")
    public CacheManager checklistCacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, Duration> ttlByCache = new HashMap<>();
        ttlByCache.put("checklistsByDam", Duration.ofMinutes(15));
        ttlByCache.put("checklistsWithAnswersByDam", Duration.ofMinutes(10));
        ttlByCache.put("checklistsWithAnswersByClient", Duration.ofMinutes(12));
        ttlByCache.put("checklistById", Duration.ofMinutes(20));
        ttlByCache.put("checklistForDam", Duration.ofMinutes(15));
        ttlByCache.put("allChecklists", Duration.ofMinutes(10));
        ttlByCache.put("allChecklistResponses", Duration.ofMinutes(10));
        ttlByCache.put("checklistResponseById", Duration.ofMinutes(20));
        ttlByCache.put("checklistResponsesByDam", Duration.ofMinutes(15));
        ttlByCache.put("checklistResponseDetail", Duration.ofMinutes(20));
        ttlByCache.put("checklistResponsesByUser", Duration.ofMinutes(15));
        ttlByCache.put("checklistResponsesByDate", Duration.ofMinutes(15));
        ttlByCache.put("damLastChecklist", Duration.ofMinutes(15));
        ttlByCache.put("checklistResponsesByDamPaged", Duration.ofMinutes(10));
        ttlByCache.put("checklistResponsesByUserPaged", Duration.ofMinutes(10));
        ttlByCache.put("checklistResponsesByDatePaged", Duration.ofMinutes(10));
        ttlByCache.put("allChecklistResponsesPaged", Duration.ofMinutes(10));
        ttlByCache.put("checklistResponsesByClient", Duration.ofMinutes(12));
        ttlByCache.put("clientLatestDetailedChecklistResponses", Duration.ofMinutes(15));

        return createRedisCacheManager(connectionFactory, ttlByCache, Duration.ofMinutes(12));
    }

    @Bean("instrumentCacheManager")
    public CacheManager instrumentCacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, Duration> ttlByCache = new HashMap<>();
        ttlByCache.put("instrumentById", Duration.ofMinutes(15));
        ttlByCache.put("instrumentWithDetails", Duration.ofMinutes(12));
        ttlByCache.put("instrumentsByClient", Duration.ofMinutes(10));
        ttlByCache.put("instrumentsByFilters", Duration.ofMinutes(10));
        ttlByCache.put("instrumentsByDam", Duration.ofMinutes(15));
        ttlByCache.put("allInstruments", Duration.ofMinutes(5));
        ttlByCache.put("instrumentResponseDTO", Duration.ofMinutes(10));

        return createRedisCacheManager(connectionFactory, ttlByCache, Duration.ofMinutes(10));
    }

    @Bean("readingCacheManager")
    public CacheManager readingCacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, Duration> ttlByCache = new HashMap<>();
        ttlByCache.put("readingsByInstrument", Duration.ofMinutes(30));
        ttlByCache.put("instrumentLimitStatus", Duration.ofMinutes(15));
        ttlByCache.put("clientInstrumentLatestGroupedReadings", Duration.ofHours(1));
        ttlByCache.put("groupedReadings", Duration.ofMinutes(30));
        ttlByCache.put("readingsByFiltersOptimized", Duration.ofMinutes(10));
        ttlByCache.put("multiInstrumentReadings", Duration.ofMinutes(15));
        ttlByCache.put("clientInstrumentLimitStatuses", Duration.ofMinutes(20));
        ttlByCache.put("readingById", Duration.ofMinutes(60));
        ttlByCache.put("readingResponseDTO", Duration.ofMinutes(30));
        ttlByCache.put("readingExists", Duration.ofMinutes(20));
        ttlByCache.put("latestReadings", Duration.ofMinutes(10));
        ttlByCache.put("readingsByOutput", Duration.ofMinutes(20));
        ttlByCache.put("readingsByFilters", Duration.ofMinutes(5));

        return createRedisCacheManager(connectionFactory, ttlByCache, Duration.ofMinutes(10));
    }
}
