package com.aetrix.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "uhi-summary",
                "uhi-heatmap",
                "uhi-hotspots",
                "vegetation-summary",
                "vegetation-map",
                "vegetation-alerts",
                "pollution-summary",
                "pollution-map",
                "pollution-hotspots",
                "forecast-trend",
                "forecast-breach",
                "action-plan",
                "action-summary",
                "dashboard-overview",
                "grok-summaries"
        ));
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .recordStats();
    }
}
