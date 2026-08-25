package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.dto.Report.LeaderboardMember;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;
import com.dat.ai_receptionist_web.util.error.LeaderboardUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION_TESTS", matches = "true")
class LeaderboardRedisStoreIntegrationTest {
    private static final int TEST_YEAR = 2999;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;
    private LeaderboardRedisStore store;
    private LeaderboardScope scope;
    private final List<String> generations = new ArrayList<>();

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                requiredEnv("SPRING_DATA_REDIS_HOST"),
                Integer.parseInt(requiredEnv("SPRING_DATA_REDIS_PORT"))
        );
        String username = System.getenv("SPRING_DATA_REDIS_USERNAME");
        String password = System.getenv("SPRING_DATA_REDIS_PASSWORD");
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder client =
                LettuceClientConfiguration.builder();
        if (Boolean.parseBoolean(System.getenv().getOrDefault("SSL_ENABLED", "false"))) {
            client.useSsl().disablePeerVerification().and();
        }

        connectionFactory = new LettuceConnectionFactory(config, client.build());
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        store = new LeaderboardRedisStore(redis, new ObjectMapper().findAndRegisterModules(), new SimpleMeterRegistry());
        scope = LeaderboardScope.fitness(TEST_YEAR, 4, SkillLevel.ADVANCED);
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void rebuildPromotesTemporaryKeysWithoutTtlOnActiveKeys() {
        String generation = generation();
        List<LeaderboardRedisStore.ProjectionEntry> entries = List.of(
                entry("TEST-001", 100.0),
                entry("TEST-002", 200.0)
        );

        store.appendRebuildBatch(scope, generation, entries);
        assertTemporaryTtl(scope.rankKey(), generation);
        assertTemporaryTtl(scope.dataKey(), generation);
        assertTemporaryTtl(scope.memberKey(), generation);

        assertEquals(2, store.completeRebuild(scope, generation, 2));
        assertPersistent(scope.rankKey());
        assertPersistent(scope.dataKey());
        assertPersistent(scope.memberKey());
        assertPersistent(scope.stateKey());
        assertCounts(2);

        LeaderboardRedisStore.Page page = store.read(scope, 0, 50);
        assertTrue(page.initialized());
        assertEquals(2, page.totalEntries());
        assertEquals(2, page.rows().size());
    }

    @Test
    void upsertAndRemoveKeepActiveProjectionPersistentAndHistoryTemporary() {
        String generation = generation();
        store.appendRebuildBatch(scope, generation, List.of(
                entry("TEST-001", 100.0),
                entry("TEST-002", 200.0)
        ));
        store.completeRebuild(scope, generation, 2);

        store.upsert(scope, "TEST-001", 300.0, Map.of("score", 300), member("TEST-001"));
        assertPersistent(scope.rankKey());
        assertPersistent(scope.dataKey());
        assertPersistent(scope.memberKey());
        assertCounts(2);

        Long historyTtl = redis.getExpire(scope.historyKey(), TimeUnit.SECONDS);
        assertNotNull(historyTtl);
        assertTrue(historyTtl > 0 && historyTtl <= TimeUnit.DAYS.toSeconds(30));

        store.remove(scope, "TEST-002");
        assertCounts(1);
        assertPersistent(scope.rankKey());
        assertPersistent(scope.dataKey());
        assertPersistent(scope.memberKey());
    }

    @Test
    void emptyRebuildClearsActiveRowsButKeepsInitializedPersistentState() {
        store.upsert(scope, "TEST-001", 100.0, Map.of("score", 100), member("TEST-001"));
        assertCounts(1);

        String generation = generation();
        assertEquals(0, store.completeRebuild(scope, generation, 0));

        assertFalse(Boolean.TRUE.equals(redis.hasKey(scope.rankKey())));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(scope.dataKey())));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(scope.memberKey())));
        assertPersistent(scope.stateKey());
        assertEquals("0", redis.<String, String>opsForHash().get(scope.stateKey(), "entryCount"));

        LeaderboardRedisStore.Page page = store.read(scope, 0, 50);
        assertTrue(page.initialized());
        assertEquals(0, page.totalEntries());
        assertTrue(page.rows().isEmpty());
    }

    @Test
    void countMismatchReportsExpectedAndActualCounts() {
        String generation = generation();
        store.appendRebuildBatch(scope, generation, List.of(
                entry("TEST-001", 100.0),
                entry("TEST-002", 200.0)
        ));
        store.completeRebuild(scope, generation, 2);
        redis.opsForHash().delete(scope.memberKey(), "TEST-002");

        LeaderboardUnavailableException exception = assertThrows(
                LeaderboardUnavailableException.class,
                () -> store.read(scope, 0, 50)
        );
        assertTrue(exception.getMessage().contains("expected=2"));
        assertTrue(exception.getMessage().contains("rank=2"));
        assertTrue(exception.getMessage().contains("data=2"));
        assertTrue(exception.getMessage().contains("member=1"));
    }

    private String generation() {
        String generation = store.startRebuild();
        generations.add(generation);
        return generation;
    }

    private LeaderboardRedisStore.ProjectionEntry entry(String studentCode, double score) {
        return new LeaderboardRedisStore.ProjectionEntry(
                studentCode,
                score,
                Map.of("score", score),
                member(studentCode)
        );
    }

    private LeaderboardMember member(String studentCode) {
        return new LeaderboardMember(UUID.randomUUID(), studentCode, "Integration " + studentCode, null);
    }

    private void assertTemporaryTtl(String activeKey, String generation) {
        Long ttl = redis.getExpire(activeKey + ":rebuild:" + generation, TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= TimeUnit.HOURS.toSeconds(24));
    }

    private void assertPersistent(String key) {
        assertEquals(-1L, redis.getExpire(key, TimeUnit.SECONDS));
    }

    private void assertCounts(long expected) {
        assertEquals(expected, redis.opsForZSet().zCard(scope.rankKey()));
        assertEquals(expected, redis.opsForHash().size(scope.dataKey()));
        assertEquals(expected, redis.opsForHash().size(scope.memberKey()));
        assertEquals(Long.toString(expected), redis.<String, String>opsForHash().get(scope.stateKey(), "entryCount"));
    }

    private void cleanup() {
        if (redis == null || scope == null) {
            return;
        }
        redis.delete(List.of(
                scope.rankKey(), scope.dataKey(), scope.memberKey(), scope.historyKey(), scope.stateKey()
        ));
        redis.opsForSet().remove(LeaderboardRedisStore.SCOPE_REGISTRY_KEY, scope.registryValue());
        for (String generation : generations) {
            redis.delete(List.of(
                    scope.rankKey() + ":rebuild:" + generation,
                    scope.dataKey() + ":rebuild:" + generation,
                    scope.memberKey() + ":rebuild:" + generation
            ));
        }
        generations.clear();
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable " + name);
        }
        return value;
    }
}
