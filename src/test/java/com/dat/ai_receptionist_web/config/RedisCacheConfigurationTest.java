package com.dat.ai_receptionist_web.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheConfigurationTest {

    @Test
    void jsonSerializer_roundTripsCachePayload() {
        RedisSerializer<Object> serializer = new RedisConfig().redisValueSerializer();
        CachePayload expectedItem = new CachePayload(UUID.randomUUID(), LocalDate.of(2026, 8, 2));

        Object deserialized = serializer.deserialize(serializer.serialize(List.of(expectedItem)));

        assertThat(deserialized).isInstanceOf(List.class);
        assertThat((List<?>) deserialized)
                .hasSize(1)
                .first()
                .isInstanceOf(CachePayload.class);
        assertThat(((CachePayload) ((List<?>) deserialized).getFirst()).assignmentId())
                .isEqualTo(expectedItem.assignmentId());
    }

    record CachePayload(UUID assignmentId, LocalDate assignedDate) {
    }

    @Test
    void cacheableMethod_queriesRepositoryOnce_thenServesCacheHit() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CachingTestConfiguration.class)) {
            ProbeRepository repository = context.getBean(ProbeRepository.class);
            ProbeService service = context.getBean(ProbeService.class);
            UUID coachId = UUID.randomUUID();

            assertThat(service.findCoachAssignments(coachId)).isEqualTo("assignment-" + coachId);
            assertThat(repository.calls).isOne();

            assertThat(service.findCoachAssignments(coachId)).isEqualTo("assignment-" + coachId);
            assertThat(repository.calls).isOne();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CachingTestConfiguration {
        @Bean(name = "redisCacheManager")
        CacheManager redisCacheManager() {
            return new ConcurrentMapCacheManager("coachAssignments");
        }

        @Bean
        ProbeRepository probeRepository() {
            return new ProbeRepository();
        }

        @Bean
        ProbeService probeService(ProbeRepository repository) {
            return new ProbeService(repository);
        }
    }

    static class ProbeRepository {
        private int calls;

        String findByCoachId(UUID coachId) {
            calls++;
            return "assignment-" + coachId;
        }
    }

    static class ProbeService {
        private final ProbeRepository repository;

        ProbeService(ProbeRepository repository) {
            this.repository = repository;
        }

        @Cacheable(value = "coachAssignments", key = "#coachId", cacheManager = "redisCacheManager")
        public String findCoachAssignments(UUID coachId) {
            return repository.findByCoachId(coachId);
        }
    }
}
