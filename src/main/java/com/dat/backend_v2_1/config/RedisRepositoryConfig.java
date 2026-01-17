package com.dat.backend_v2_1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Cấu hình cho Redis Repositories
 * Chỉ quét các repository trong package redis
 */
@Configuration
@EnableRedisRepositories(
    basePackages = "com.dat.backend_v2_0.redis"
)
public class RedisRepositoryConfig {
}
