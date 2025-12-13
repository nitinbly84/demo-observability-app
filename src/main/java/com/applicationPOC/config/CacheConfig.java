package com.applicationPOC.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CacheConfig {

    @Bean
    @Profile("dev")
    CacheManager devCacheManager() {
        return new ConcurrentMapCacheManager("demoCacheDev");
    }

    @Bean
    @Profile("prod")
    CacheManager prodCacheManager() {
        return new ConcurrentMapCacheManager("demoCacheProd");
    }
}