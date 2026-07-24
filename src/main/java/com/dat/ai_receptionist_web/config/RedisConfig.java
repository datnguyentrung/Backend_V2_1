package com.dat.ai_receptionist_web.config;

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
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
@EnableAsync
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

        // Chỉ set user/pass nếu trong .env có (phòng lỗi null)
        if (redisUsername != null && !redisUsername.isEmpty()) {
            config.setUsername(redisUsername);
        }
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        // ĐÂY LÀ ĐOẠN QUYẾT ĐỊNH SỰ SỐNG CÒN CỦA KẾT NỐI
        // Nếu thấy chữ render.com thì ép nó dùng SSL và TẮT kiểm tra chứng chỉ
        if (redisHost != null && redisHost.contains("render.com")) {
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .useSsl() // Ép bật SSL
                    .disablePeerVerification() // Tắt kiểm tra chứng chỉ (Bắt buộc phải có dòng này)
                    .build();
            return new LettuceConnectionFactory(config, clientConfig);
        }

        // Còn nếu chạy localhost ở máy nhà ông thì cứ chạy bình thường không cần SSL
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
        // Tận dụng lại ObjectMapper đã config ở trên để //@Cacheable cũng lưu JSON chuẩn như RedisTemplate (Spring Boot 4.0+)
        RedisSerializer<Object> serializer = RedisSerializer.json();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(7)) // TTL mặc định 7 ngày
                .disableCachingNullValues()
                // Thêm prefix "app_name:" hoặc để default "::" tùy bạn, ở đây mình config dùng dấu ":" cho đẹp
                .computePrefixWith(cacheName -> cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
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

    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory connectionFactory, CacheTtlConfig ttlConfig) {

        // 1. Lấy cấu hình mặc định (dùng JSON serializer và TTL 7 ngày mà bạn đã setup)
        RedisCacheConfiguration defaultConfig = cacheConfiguration();

        // 2. Map riêng từng TTL cho từng loại Tên Cache (Tên khai báo trong //@Cacheable)
        Map<String, RedisCacheConfiguration> specificCacheConfigs = new HashMap<>();

        // ===== NHÓM 1: DỮ LIỆU ĐỘNG (Thay đổi liên tục) -> TTL: 1 NGÀY =====
        // Các detail thường chứa Sĩ số, Phân công... nên cho chết sớm để update
        specificCacheConfigs.put("coachDetail", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("coachDetailByCode", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("classScheduleDetail", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("studentEnrollmentsById", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("studentEnrollmentsByCode", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("studentEnrollmentsByClass", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("singleEnrollment", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("fcmTokensByRole", defaultConfig.entryTtl(ttlConfig.randomOneHour()));

        // ===== NHÓM 2: DỮ LIỆU ÍT ĐỔI (Thông tin cơ bản) -> TTL: 1 TUẦN =====
        specificCacheConfigs.put("coach", defaultConfig.entryTtl(ttlConfig.randomOneWeek()));
        specificCacheConfigs.put("coachByCode", defaultConfig.entryTtl(ttlConfig.randomOneWeek()));
        specificCacheConfigs.put("classSchedule", defaultConfig.entryTtl(ttlConfig.randomOneWeek()));

        // 3. Build CacheManager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig) // Nếu quên cấu hình tên nào, nó sẽ dùng mặc định 7 ngày
                .withInitialCacheConfigurations(specificCacheConfigs) // Nạp cấu hình riêng vào
                .build();
    }
}
