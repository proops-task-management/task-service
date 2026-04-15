package com.proops2026.taskservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            ObjectMapper objectMapper,
            @Value("${task.cache.ttl-seconds:30}") long listTtlSeconds) {
        long safeListTtlSeconds = Math.max(listTtlSeconds, 1);
        long taskTtlSeconds = safeListTtlSeconds * 2;

        return builder -> builder
                .withCacheConfiguration("tasks", withTtlSeconds(objectMapper, safeListTtlSeconds))
                .withCacheConfiguration("task", withTtlSeconds(objectMapper, taskTtlSeconds));
    }

    private RedisCacheConfiguration withTtlSeconds(ObjectMapper objectMapper, long ttlSeconds) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .disableCachingNullValues();
    }
}
