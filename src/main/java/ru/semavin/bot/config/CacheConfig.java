package ru.semavin.bot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.TimeUnit;


@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.setAsyncCacheMode(true);

        cacheManager.registerCustomCache("schedule_day",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        .maximumSize(500)
                        .buildAsync());

        cacheManager.registerCustomCache("schedule_week",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        .maximumSize(500)
                        .buildAsync());
        cacheManager.registerCustomCache("deadlineMessages",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.DAYS)
                        .maximumSize(15)
                        .buildAsync());
        cacheManager.registerCustomCache("users",
                Caffeine.newBuilder()
                        .expireAfterWrite(3, TimeUnit.DAYS)
                        .maximumSize(1000)
                        .buildAsync());
        cacheManager.registerCustomCache("studentGroupList",
                Caffeine.newBuilder()
                        .expireAfterWrite(14, TimeUnit.DAYS)
                        .maximumSize(1000)
                        .buildAsync());

        return cacheManager;
    }
}
