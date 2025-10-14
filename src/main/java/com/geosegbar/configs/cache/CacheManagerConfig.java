package com.geosegbar.configs.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

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

    private CaffeineCache buildCache(String name, Duration ttlWrite, Duration ttlAccess, long maxSize) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(ttlWrite)
                        .expireAfterAccess(ttlAccess)
                        .build());
    }

    @Bean("instrumentGraphCacheManager")
    @Primary
    public CacheManager instrumentGraphCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                buildCache("graphPatternsByDam", Duration.ofMinutes(20), Duration.ofMinutes(10), 50),
                buildCache("graphPatternById", Duration.ofMinutes(15), Duration.ofMinutes(8), 300),
                buildCache("graphPatternsByInstrument", Duration.ofMinutes(15), Duration.ofMinutes(8), 100),
                buildCache("folderWithPatterns", Duration.ofMinutes(18), Duration.ofMinutes(10), 80),
                buildCache("damFoldersWithPatterns", Duration.ofMinutes(25), Duration.ofMinutes(12), 25),
                buildCache("graphProperties", Duration.ofMinutes(12), Duration.ofMinutes(6), 400),
                buildCache("graphAxes", Duration.ofMinutes(20), Duration.ofMinutes(10), 200)
        ));
        return cacheManager;
    }

    @Bean("instrumentTabulateCacheManager")
    public CacheManager instrumentTabulateCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                buildCache("tabulatePatterns", Duration.ofMinutes(15), Duration.ofMinutes(8), 300),
                buildCache("tabulatePatternsByDam", Duration.ofMinutes(20), Duration.ofMinutes(10), 50),
                buildCache("tabulatePatternsByFolder", Duration.ofMinutes(15), Duration.ofMinutes(8), 80),
                buildCache("tabulateFolderWithPatterns", Duration.ofMinutes(18), Duration.ofMinutes(10), 80),
                buildCache("damTabulateFoldersWithPatterns", Duration.ofMinutes(25), Duration.ofMinutes(12), 25)
        ));

        return cacheManager;
    }

    @Bean("checklistCacheManager")
    public CacheManager checklistCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                buildCache("checklistsByDam", Duration.ofMinutes(15), Duration.ofMinutes(8), 100),
                buildCache("checklistsWithAnswersByDam", Duration.ofMinutes(10), Duration.ofMinutes(5), 50),
                buildCache("checklistsWithAnswersByClient", Duration.ofMinutes(12), Duration.ofMinutes(6), 30),
                buildCache("checklistById", Duration.ofMinutes(20), Duration.ofMinutes(10), 200),
                buildCache("checklistForDam", Duration.ofMinutes(15), Duration.ofMinutes(8), 100),
                buildCache("allChecklists", Duration.ofMinutes(10), Duration.ofMinutes(5), 5),
                buildCache("allChecklistResponses", Duration.ofMinutes(10), Duration.ofMinutes(5), 5),
                buildCache("checklistResponseById", Duration.ofMinutes(20), Duration.ofMinutes(10), 200),
                buildCache("checklistResponsesByDam", Duration.ofMinutes(15), Duration.ofMinutes(8), 100),
                buildCache("checklistResponseDetail", Duration.ofMinutes(20), Duration.ofMinutes(10), 200),
                buildCache("checklistResponsesByUser", Duration.ofMinutes(15), Duration.ofMinutes(8), 100),
                buildCache("checklistResponsesByDate", Duration.ofMinutes(15), Duration.ofMinutes(8), 100),
                buildCache("damLastChecklist", Duration.ofMinutes(15), Duration.ofMinutes(8), 100),
                buildCache("checklistResponsesByDamPaged", Duration.ofMinutes(10), Duration.ofMinutes(5), 100),
                buildCache("checklistResponsesByUserPaged", Duration.ofMinutes(10), Duration.ofMinutes(5), 100),
                buildCache("checklistResponsesByDatePaged", Duration.ofMinutes(10), Duration.ofMinutes(5), 100),
                buildCache("allChecklistResponsesPaged", Duration.ofMinutes(10), Duration.ofMinutes(5), 5),
                buildCache("checklistResponsesByClient", Duration.ofMinutes(12), Duration.ofMinutes(6), 50),
                buildCache("clientLatestDetailedChecklistResponses", Duration.ofMinutes(15), Duration.ofMinutes(10), 100)
        ));

        return cacheManager;
    }

    @Bean("instrumentCacheManager")
    public CacheManager instrumentCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                buildCache("instrumentById", Duration.ofMinutes(15), Duration.ofMinutes(10), 300),
                buildCache("instrumentWithDetails", Duration.ofMinutes(12), Duration.ofMinutes(8), 100),
                buildCache("instrumentsByClient", Duration.ofMinutes(10), Duration.ofMinutes(5), 50),
                buildCache("instrumentsByFilters", Duration.ofMinutes(10), Duration.ofMinutes(5), 200),
                buildCache("instrumentsByDam", Duration.ofMinutes(15), Duration.ofMinutes(8), 100),
                buildCache("allInstruments", Duration.ofMinutes(5), Duration.ofMinutes(3), 5),
                buildCache("instrumentResponseDTO", Duration.ofMinutes(10), Duration.ofMinutes(5), 500)
        ));
        return cacheManager;
    }

    @Bean(name = "readingCacheManager")
    public CacheManager readingCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        List<CaffeineCache> caches = Arrays.asList(
                buildCache("readingsByInstrument", Duration.ofMinutes(30), Duration.ofMinutes(30), 1000),
                buildCache("instrumentLimitStatus", Duration.ofMinutes(15), Duration.ofMinutes(15), 500),
                buildCache("clientInstrumentLatestGroupedReadings", Duration.ofHours(1), Duration.ofHours(1), 100),
                buildCache("groupedReadings", Duration.ofMinutes(30), Duration.ofMinutes(30), 200),
                buildCache("readingsByFiltersOptimized", Duration.ofMinutes(10), Duration.ofMinutes(10), 500),
                buildCache("multiInstrumentReadings", Duration.ofMinutes(15), Duration.ofMinutes(15), 200),
                buildCache("clientInstrumentLimitStatuses", Duration.ofMinutes(20), Duration.ofMinutes(20), 100),
                buildCache("readingById", Duration.ofMinutes(60), Duration.ofMinutes(60), 5000),
                buildCache("readingResponseDTO", Duration.ofMinutes(30), Duration.ofMinutes(30), 3000),
                buildCache("readingExists", Duration.ofMinutes(20), Duration.ofMinutes(20), 1000),
                buildCache("latestReadings", Duration.ofMinutes(10), Duration.ofMinutes(10), 500),
                buildCache("readingsByOutput", Duration.ofMinutes(20), Duration.ofMinutes(20), 300),
                buildCache("readingsByFilters", Duration.ofMinutes(5), Duration.ofMinutes(5), 300)
        );

        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
