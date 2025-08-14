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
                                .recordStats()
                                .build()),
                new CaffeineCache("graphPatternById",
                        Caffeine.newBuilder()
                                .maximumSize(300)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .recordStats()
                                .build()),
                new CaffeineCache("graphPatternsByInstrument",
                        Caffeine.newBuilder()
                                .maximumSize(100)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .recordStats()
                                .build()),
                new CaffeineCache("folderWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(80)
                                .expireAfterWrite(Duration.ofMinutes(18))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .recordStats()
                                .build()),
                new CaffeineCache("damFoldersWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(25)
                                .expireAfterWrite(Duration.ofMinutes(25))
                                .expireAfterAccess(Duration.ofMinutes(12))
                                .recordStats()
                                .build()),
                new CaffeineCache("graphProperties",
                        Caffeine.newBuilder()
                                .maximumSize(400)
                                .expireAfterWrite(Duration.ofMinutes(12))
                                .expireAfterAccess(Duration.ofMinutes(6))
                                .recordStats()
                                .build()),
                new CaffeineCache("graphAxes",
                        Caffeine.newBuilder()
                                .maximumSize(200)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .recordStats()
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
                                .recordStats()
                                .build()),
                new CaffeineCache("tabulatePatternsByDam",
                        Caffeine.newBuilder()
                                .maximumSize(50)
                                .expireAfterWrite(Duration.ofMinutes(20))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .recordStats()
                                .build()),
                new CaffeineCache("tabulatePatternsByFolder",
                        Caffeine.newBuilder()
                                .maximumSize(80)
                                .expireAfterWrite(Duration.ofMinutes(15))
                                .expireAfterAccess(Duration.ofMinutes(8))
                                .recordStats()
                                .build()),
                new CaffeineCache("tabulateFolderWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(80)
                                .expireAfterWrite(Duration.ofMinutes(18))
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .recordStats()
                                .build()),
                new CaffeineCache("damTabulateFoldersWithPatterns",
                        Caffeine.newBuilder()
                                .maximumSize(25)
                                .expireAfterWrite(Duration.ofMinutes(25))
                                .expireAfterAccess(Duration.ofMinutes(12))
                                .recordStats()
                                .build())
        ));

        return cacheManager;
    }
}
