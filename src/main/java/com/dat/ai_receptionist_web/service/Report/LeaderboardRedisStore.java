package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.dto.Report.LeaderboardMember;
import com.dat.ai_receptionist_web.util.error.LeaderboardUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LeaderboardRedisStore {
    public static final String SCOPE_REGISTRY_KEY = "leaderboard:scopes";

    private static final DefaultRedisScript<Long> UPSERT_SCRIPT = new DefaultRedisScript<>("""
            local oldScore = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if ARGV[5] == '1' and oldScore and tonumber(oldScore) ~= tonumber(ARGV[2]) then
              local oldRank = redis.call('ZREVRANK', KEYS[1], ARGV[1])
              if oldRank then
                redis.call('HSET', KEYS[4], ARGV[1], tostring(oldRank + 1))
                redis.call('EXPIRE', KEYS[4], 2592000)
              end
            end
            redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
            redis.call('HSET', KEYS[2], ARGV[1], ARGV[3])
            redis.call('HSET', KEYS[3], ARGV[1], ARGV[4])
            local count = redis.call('ZCARD', KEYS[1])
            redis.call('HSET', KEYS[5], 'initialized', 'true', 'entryCount', tostring(count), 'updatedAt', ARGV[6])
            redis.call('SADD', KEYS[6], ARGV[7])
            return count
            """, Long.class);

    private static final DefaultRedisScript<Long> REMOVE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('ZREM', KEYS[1], ARGV[1])
            redis.call('HDEL', KEYS[2], ARGV[1])
            redis.call('HDEL', KEYS[3], ARGV[1])
            if ARGV[2] == '1' then redis.call('HDEL', KEYS[4], ARGV[1]) end
            local count = redis.call('ZCARD', KEYS[1])
            redis.call('HSET', KEYS[5], 'initialized', 'true', 'entryCount', tostring(count), 'updatedAt', ARGV[3])
            redis.call('SADD', KEYS[6], ARGV[4])
            return count
            """, Long.class);

    private static final DefaultRedisScript<Long> SWAP_SCRIPT = new DefaultRedisScript<>("""
            local rankCount = redis.call('ZCARD', KEYS[1])
            local dataCount = redis.call('HLEN', KEYS[2])
            local memberCount = redis.call('HLEN', KEYS[3])
            if rankCount ~= tonumber(ARGV[1]) or dataCount ~= rankCount or memberCount ~= rankCount then
              return redis.error_reply('leaderboard rebuild validation failed')
            end
            redis.call('DEL', KEYS[4], KEYS[5], KEYS[6])
            if rankCount > 0 then
              redis.call('RENAME', KEYS[1], KEYS[4])
              redis.call('RENAME', KEYS[2], KEYS[5])
              redis.call('RENAME', KEYS[3], KEYS[6])
              redis.call('PERSIST', KEYS[4])
              redis.call('PERSIST', KEYS[5])
              redis.call('PERSIST', KEYS[6])
            else
              redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
            end
            if ARGV[2] == '1' then redis.call('DEL', KEYS[7]) end
            redis.call('HSET', KEYS[8], 'initialized', 'true', 'entryCount', tostring(rankCount),
                       'updatedAt', ARGV[3], 'generation', ARGV[4])
            redis.call('SADD', KEYS[9], ARGV[5])
            return rankCount
            """, Long.class);

    private static final DefaultRedisScript<List> READ_SCRIPT = new DefaultRedisScript<>("""
            local initialized = redis.call('HGET', KEYS[5], 'initialized')
            if initialized ~= 'true' then
              if redis.call('EXISTS', KEYS[1]) == 1 or redis.call('EXISTS', KEYS[2]) == 1
                  or redis.call('EXISTS', KEYS[3]) == 1 then
                return {'invalid', 'state_missing'}
              end
              return {'uninitialized', '0'}
            end
            local expected = tonumber(redis.call('HGET', KEYS[5], 'entryCount'))
            if expected == nil then return {'invalid', 'entry_count'} end
            local rankCount = redis.call('ZCARD', KEYS[1])
            local dataCount = redis.call('HLEN', KEYS[2])
            local memberCount = redis.call('HLEN', KEYS[3])
            if rankCount ~= expected or dataCount ~= rankCount or memberCount ~= rankCount then
              return {'invalid', 'count_mismatch', tostring(expected), tostring(rankCount), tostring(dataCount), tostring(memberCount)}
            end
            local result = {'initialized', tostring(rankCount)}
            local codes = redis.call('ZREVRANGE', KEYS[1], ARGV[1], ARGV[2])
            for _, code in ipairs(codes) do
              local data = redis.call('HGET', KEYS[2], code)
              local member = redis.call('HGET', KEYS[3], code)
              if not data or not member then return {'invalid', 'incomplete_row'} end
              local history = ''
              if ARGV[3] == '1' then history = redis.call('HGET', KEYS[4], code) or '' end
              table.insert(result, code)
              table.insert(result, data)
              table.insert(result, member)
              table.insert(result, history)
            end
            return result
            """, List.class);

    private static final DefaultRedisScript<Long> UPDATE_MEMBER_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('ZSCORE', KEYS[1], ARGV[1]) then
              redis.call('HSET', KEYS[2], ARGV[1], ARGV[2])
              redis.call('HSET', KEYS[3], 'updatedAt', ARGV[3])
              return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public void upsert(LeaderboardScope scope, String studentCode, double score, Object data, LeaderboardMember member) {
        execute(() -> redisTemplate.execute(
                UPSERT_SCRIPT,
                keys(scope),
                studentCode,
                Double.toString(score),
                json(data),
                json(member),
                scope.type() == LeaderboardScope.Type.FITNESS ? "1" : "0",
                Instant.now().toString(),
                scope.registryValue()
        ));
    }

    public void remove(LeaderboardScope scope, String studentCode) {
        execute(() -> redisTemplate.execute(
                REMOVE_SCRIPT,
                keys(scope),
                studentCode,
                scope.type() == LeaderboardScope.Type.FITNESS ? "1" : "0",
                Instant.now().toString(),
                scope.registryValue()
        ));
    }

    public void updateMemberIfPresent(LeaderboardScope scope, LeaderboardMember member) {
        execute(() -> redisTemplate.execute(
                UPDATE_MEMBER_SCRIPT,
                List.of(scope.rankKey(), scope.memberKey(), scope.stateKey()),
                member.studentCode(), json(member), Instant.now().toString()
        ));
    }

    public Page read(LeaderboardScope scope, long offset, int pageSize) {
        return execute(() -> {
            List<?> snapshot = redisTemplate.execute(
                    READ_SCRIPT,
                    List.of(scope.rankKey(), scope.dataKey(), scope.memberKey(), scope.historyKey(), scope.stateKey()),
                    Long.toString(offset),
                    Long.toString(offset + pageSize - 1),
                    scope.type() == LeaderboardScope.Type.FITNESS ? "1" : "0"
            );
            if (snapshot == null || snapshot.size() < 2) {
                throw integrityFailure(scope, "empty_snapshot");
            }
            String state = snapshot.get(0).toString();
            if ("uninitialized".equals(state)) {
                return Page.uninitialized();
            }
            if ("invalid".equals(state)) {
                String reason = snapshot.get(1).toString();
                if ("count_mismatch".equals(reason) && snapshot.size() >= 6) {
                    throw integrityCountMismatchFailure(
                            scope,
                            parseInteger(snapshot.get(2), scope, "expected_count"),
                            parseInteger(snapshot.get(3), scope, "rank_count"),
                            parseInteger(snapshot.get(4), scope, "data_count"),
                            parseInteger(snapshot.get(5), scope, "member_count")
                    );
                }
                throw integrityFailure(scope, reason);
            }
            if (!"initialized".equals(state)) {
                throw integrityFailure(scope, "unknown_state");
            }
            int rankCount = parseInteger(snapshot.get(1), scope, "entry_count");
            if ((snapshot.size() - 2) % 4 != 0) {
                throw integrityFailure(scope, "invalid_snapshot_shape");
            }
            List<Row> rows = new ArrayList<>((snapshot.size() - 2) / 4);
            for (int i = 2; i < snapshot.size(); i += 4) {
                String history = snapshot.get(i + 3).toString();
                Integer rankBefore = history.isEmpty() ? null : parseInteger(history, scope, "rank_before");
                rows.add(new Row(
                        snapshot.get(i).toString(), snapshot.get(i + 1).toString(),
                        snapshot.get(i + 2).toString(), rankBefore
                ));
            }
            return new Page(true, rankCount, rows);
        });
    }

    public List<LeaderboardScope> registeredScopes() {
        return execute(() -> redisTemplate.execute((RedisConnection connection) -> {
            List<LeaderboardScope> scopes = new ArrayList<>();
            byte[] key = redisTemplate.getStringSerializer().serialize(SCOPE_REGISTRY_KEY);
            try (Cursor<byte[]> cursor = connection.setCommands().sScan(key, ScanOptions.scanOptions().count(100).build())) {
                while (cursor.hasNext()) {
                    scopes.add(LeaderboardScope.fromRegistryValue(
                            redisTemplate.getStringSerializer().deserialize(cursor.next())
                    ));
                }
            }
            return scopes;
        }));
    }

    public String startRebuild() {
        return UUID.randomUUID().toString();
    }

    public void appendRebuildBatch(LeaderboardScope scope, String generation, List<ProjectionEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        execute(() -> redisTemplate.executePipelined(new org.springframework.data.redis.core.SessionCallback<>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Object execute(org.springframework.data.redis.core.RedisOperations operations) {
                String rankKey = temporaryKey(scope.rankKey(), generation);
                String dataKey = temporaryKey(scope.dataKey(), generation);
                String memberKey = temporaryKey(scope.memberKey(), generation);
                for (ProjectionEntry entry : entries) {
                    operations.opsForZSet().add(rankKey, entry.studentCode(), entry.score());
                    operations.opsForHash().put(dataKey, entry.studentCode(), json(entry.data()));
                    operations.opsForHash().put(memberKey, entry.studentCode(), json(entry.member()));
                }
                Duration temporaryKeyTtl = Duration.ofHours(24);
                operations.expire(rankKey, temporaryKeyTtl);
                operations.expire(dataKey, temporaryKeyTtl);
                operations.expire(memberKey, temporaryKeyTtl);
                return null;
            }
        }));
    }

    public int completeRebuild(LeaderboardScope scope, String generation, int expectedCount) {
        List<String> keys = List.of(
                temporaryKey(scope.rankKey(), generation),
                temporaryKey(scope.dataKey(), generation),
                temporaryKey(scope.memberKey(), generation),
                scope.rankKey(), scope.dataKey(), scope.memberKey(), scope.historyKey(),
                scope.stateKey(), SCOPE_REGISTRY_KEY
        );
        Long result = execute(() -> redisTemplate.execute(
                SWAP_SCRIPT,
                keys,
                Integer.toString(expectedCount),
                scope.type() == LeaderboardScope.Type.FITNESS ? "1" : "0",
                Instant.now().toString(),
                generation,
                scope.registryValue()
        ));
        return result == null ? 0 : result.intValue();
    }

    public void abortRebuild(LeaderboardScope scope, String generation) {
        execute(() -> redisTemplate.delete(List.of(
                temporaryKey(scope.rankKey(), generation),
                temporaryKey(scope.dataKey(), generation),
                temporaryKey(scope.memberKey(), generation)
        )));
    }

    public <T> T decode(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new LeaderboardUnavailableException("Invalid leaderboard projection data", exception);
        }
    }

    private List<String> keys(LeaderboardScope scope) {
        return List.of(
                scope.rankKey(), scope.dataKey(), scope.memberKey(), scope.historyKey(),
                scope.stateKey(), SCOPE_REGISTRY_KEY
        );
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize leaderboard projection", exception);
        }
    }

    private String temporaryKey(String activeKey, String generation) {
        return activeKey + ":rebuild:" + generation;
    }

    private int parseInteger(Object value, LeaderboardScope scope, String field) {
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            throw integrityFailure(scope, "invalid_" + field);
        }
    }

    private LeaderboardUnavailableException integrityCountMismatchFailure(
            LeaderboardScope scope, int expected, int rankCount, int dataCount, int memberCount) {
        meterRegistry.counter(
                "leaderboard.redis.integrity", "type", scope.type().name().toLowerCase(), "reason", "count_mismatch"
        ).increment();
        return new LeaderboardUnavailableException(
                "Leaderboard projection integrity check failed for " + scope.registryValue()
                        + ": count_mismatch expected=" + expected
                        + " rank=" + rankCount
                        + " data=" + dataCount
                        + " member=" + memberCount
        );
    }

    private LeaderboardUnavailableException integrityFailure(LeaderboardScope scope, String reason) {
        meterRegistry.counter(
                "leaderboard.redis.integrity", "type", scope.type().name().toLowerCase(), "reason", reason
        ).increment();
        return new LeaderboardUnavailableException(
                "Leaderboard projection integrity check failed for " + scope.registryValue() + ": " + reason
        );
    }

    private <T> T execute(RedisOperation<T> operation) {
        try {
            return operation.execute();
        } catch (LeaderboardUnavailableException exception) {
            throw exception;
        } catch (RedisConnectionFailureException exception) {
            meterRegistry.counter("leaderboard.redis.operation", "result", "connection_failure").increment();
            throw new LeaderboardUnavailableException("Redis is unavailable", exception);
        } catch (DataAccessException exception) {
            meterRegistry.counter("leaderboard.redis.operation", "result", "failure").increment();
            throw new LeaderboardUnavailableException("Leaderboard storage operation failed", exception);
        }
    }

    @FunctionalInterface
    private interface RedisOperation<T> {
        T execute();
    }

    public record Row(String studentCode, String dataJson, String memberJson, Integer rankBefore) {
    }

    public record Page(boolean initialized, int totalEntries, List<Row> rows) {
        static Page uninitialized() {
            return new Page(false, 0, List.of());
        }
    }

    public record ProjectionEntry(String studentCode, double score, Object data, LeaderboardMember member) {
    }
}
