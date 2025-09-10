package com.geosegbar.configs.cache;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheManagerConfig {

    @Bean("instrumentGraphCacheManager")
    @Primary
    public CacheManager instrumentGraphCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new CaffeineCache("graphPatternsByDam",
                        Caffeine.newBuilder()
                                .maximumSize(50)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("graphPatternById",
                        Caffeine.newBuilder()
                                .maximumSize(300)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("graphPatternsByInstrument",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("folderWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(80)
                                .expireAfterWrite(Duration.ofMinutes(18))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("damFoldersWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(25)
                                .expireAfterWrite(Duration.ofMinutes(25))
                                .expireAfterAccess(Duration.ofMinutes(12))
                                .build()),
                new CaffeineCache("graphProperties",
                        Caffeine.newBuilder()
                                .maximumSize(400)
                                .expireAfterWrite(Duration.ofMinutes(12))
                                .expireAfterAccess(Duration.ofMinutes(6))
                                .build()),
                new CaffeineCache("graphAxes",
                        Caffeine.newBuilder()
                                .maximumSize(200)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build())
        ));
        return cacheManager;
    }

    @Bean("instrumentTabulateCacheManager")
    public CacheManager instrumentTabulateCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new CaffeineCache("tabulatePatterns",
                        Caffeine.newBuilder()
                                .maximumSize(300)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("tabulatePatternsByDam",
                        Caffeine.newBuilder()
                                .maximumSize(50)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("tabulatePatternsByFolder",
                        Caffeine.newBuilder()
                                .maximumSize(80)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("tabulateFolderWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(80)
                                .expireAfterWrite(Duration.ofMinutes(18))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("damTabulateFoldersWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(25)
                                .expireAfterWrite(Duration.ofMinutes(25))
                                .expireAfterAccess(Duration.ofMinutes(12))
                                .build())
        ));

        return cacheManager;
    }

    @Bean("userCacheManager")
    public CacheManager userCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new CaffeineCache("userById",
                        Caffeine.newBuilder()
                                .maximumSize(500)
                                .expireAfterWrite(Duration.ofMinutes(30))
                                .expireAfterAccess(Duration.ofMinutes(15))
                                .build()),
                new CaffeineCache("userByEmail",
                        Caffeine.newBuilder()
                                .maximumSize(300)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("allUsers",
                        Caffeine.newBuilder()
                                .maximumSize(10)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                new CaffeineCache("usersByRoleAndClient",
                        Caffeine.newBuilder()
                                .maximumSize(50)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("usersByCreatedBy",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("userExistence",
                        Caffeine.newBuilder()
                                .maximumSize(200)
                                .expireAfterWrite(Duration.ofMinutes(5))
                                .expireAfterAccess(Duration.ofMinutes(2))
                                .build())
        ));

        return cacheManager;
    }

    @Bean("checklistCacheManager")
    public CacheManager checklistCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                // Caches existentes para ChecklistService
                new CaffeineCache("checklistsByDam",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("checklistsWithAnswersByDam",
                        Caffeine.newBuilder()
                                .maximumSize(50)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                new CaffeineCache("checklistsWithAnswersByClient",
                        Caffeine.newBuilder()
                                .maximumSize(30)
                                .expireAfterWrite(Duration.ofMinutes(12))
                                .expireAfterAccess(Duration.ofMinutes(6))
                                .build()),
                new CaffeineCache("checklistById",
                        Caffeine.newBuilder()
                                .maximumSize(200)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("checklistForDam",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("allChecklists",
                        Caffeine.newBuilder()
                                .maximumSize(5)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                // Novos caches para ChecklistResponseService
                new CaffeineCache("allChecklistResponses",
                        Caffeine.newBuilder()
                                .maximumSize(5)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                new CaffeineCache("checklistResponseById",
                        Caffeine.newBuilder()
                                .maximumSize(200)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("checklistResponsesByDam",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("checklistResponseDetail",
                        Caffeine.newBuilder()
                                .maximumSize(200)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("checklistResponsesByUser",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("checklistResponsesByDate",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("damLastChecklist",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("checklistResponsesByDamPaged",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                new CaffeineCache("checklistResponsesByUserPaged",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                new CaffeineCache("checklistResponsesByDatePaged",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                new CaffeineCache("allChecklistResponsesPaged",
                        Caffeine.newBuilder()
                                .maximumSize(5)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                new CaffeineCache("checklistResponsesByClient",
                        Caffeine.newBuilder()
                                .maximumSize(50)
                                .expireAfterWrite(Duration.ofMinutes(12))
                                .expireAfterAccess(Duration.ofMinutes(6))
                                .build())
        ));

        return cacheManager;
    }

    @Bean("instrumentCacheManager")
    public CacheManager instrumentCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new CaffeineCache("instrumentById",
                        Caffeine.newBuilder()
                                .maximumSize(300)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                new CaffeineCache("instrumentWithDetails",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(12))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("instrumentsByClient",
                        Caffeine.newBuilder()
                                .maximumSize(50)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                new CaffeineCache("instrumentsByFilters",
                        Caffeine.newBuilder()
                                .maximumSize(200)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build()),
                new CaffeineCache("instrumentsByDam",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .build()),
                new CaffeineCache("allInstruments",
                        Caffeine.newBuilder()
                                .maximumSize(5)
                                .expireAfterWrite(Duration.ofMinutes(5))
                                .expireAfterAccess(Duration.ofMinutes(3))
                                .build()),
                new CaffeineCache("instrumentResponseDTO",
                        Caffeine.newBuilder()
                                .maximumSize(500)
                                .expireAfterWrite(Duration.ofMinutes(10))
                                .expireAfterAccess(Duration.ofMinutes(5))
                                .build())
        ));
        return cacheManager;
    }
}
