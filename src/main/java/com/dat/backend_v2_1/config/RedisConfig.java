package com.dat.backend_v2_1.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig implements CachingConfigurer {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setUsername(redisUsername);
        config.setPassword(redisPassword);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(lettuceConnectionFactory());

        // ✅ Dùng RedisSerializer.json() với ObjectMapper riêng cho Redis (Spring Boot 4.0+)
        RedisSerializer<Object> serializer = RedisSerializer.json();

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, String> customStringRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(lettuceConnectionFactory());

        // Sử dụng StringRedisSerializer cho cả key và value
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        // Tận dụng lại ObjectMapper đã config ở trên để @Cacheable cũng lưu JSON chuẩn như RedisTemplate (Spring Boot 4.0+)
        RedisSerializer<Object> serializer = RedisSerializer.json();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(7)) // TTL mặc định 7 ngày
                .disableCachingNullValues()
                // Thêm prefix "app_name:" hoặc để default "::" tùy bạn, ở đây mình config dùng dấu ":" cho đẹp
                .computePrefixWith(cacheName -> cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    @Bean
    public CacheManager cacheManager() {
        return RedisCacheManager.builder(lettuceConnectionFactory())
                .cacheDefaults(cacheConfiguration())
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            @SuppressWarnings("NullableProblems")
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("⚠️ Redis GET error (Key: {}): {}", key, exception.getMessage());
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("⚠️ Redis PUT error (Key: {}): {}", key, exception.getMessage());
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("⚠️ Redis EVICT error (Key: {}): {}", key, exception.getMessage());
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("⚠️ Redis CLEAR error: {}", exception.getMessage());
            }
        };
    }
}