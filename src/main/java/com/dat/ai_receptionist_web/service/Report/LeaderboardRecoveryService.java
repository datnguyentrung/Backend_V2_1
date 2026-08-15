package com.dat.ai_receptionist_web.service.Report;

import com.dat.ai_receptionist_web.service.Projection.ProjectionOutboxService;
import com.dat.ai_receptionist_web.service.Projection.ProjectionScopeStateService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardRecoveryService {
    private static final int REDIS_BATCH_SIZE = 500;
    private static final Duration WORKER_DRAIN_TIMEOUT = Duration.ofSeconds(75);

    private final LeaderboardRebuildSnapshotService snapshotService;
    private final LeaderboardRedisStore redisStore;
    private final ProjectionScopeStateService scopeStateService;
    private final ProjectionOutboxService outboxService;
    private final MeterRegistry meterRegistry;

    public RebuildResult rebuild(LeaderboardScope scope) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String generation = redisStore.startRebuild();
        String scopeKey = scope.registryValue();
        boolean barrierStarted = false;
        log.info("LEADERBOARD_REBUILD_STARTED scope={} generation={}", scopeKey, generation);
        try {
            scopeStateService.beginRebuild(scopeKey, generation);
            barrierStarted = true;
            awaitWorkersIdle(scope);

            List<LeaderboardRedisStore.ProjectionEntry> snapshot = snapshotService.load(scope);
            appendSnapshot(scope, generation, snapshot);
            int stored = redisStore.completeRebuild(scope, generation, snapshot.size());

            meterRegistry.counter("leaderboard.rebuild", "type", scope.type().name().toLowerCase(), "result", "success")
                    .increment();
            log.info("LEADERBOARD_REBUILD_COMPLETED scope={} generation={} entries={}", scopeKey, generation, stored);
            return new RebuildResult(scope, generation, stored);
        } catch (RuntimeException exception) {
            try {
                redisStore.abortRebuild(scope, generation);
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            meterRegistry.counter("leaderboard.rebuild", "type", scope.type().name().toLowerCase(), "result", "failure")
                    .increment();
            log.error("LEADERBOARD_REBUILD_FAILED scope={} generation={}", scopeKey, generation, exception);
            throw exception;
        } finally {
            if (barrierStarted) {
                scopeStateService.endRebuild(scopeKey, generation);
            }
            timer.stop(meterRegistry.timer("leaderboard.rebuild.duration", "type", scope.type().name().toLowerCase()));
        }
    }

    private void appendSnapshot(LeaderboardScope scope, String generation,
                                List<LeaderboardRedisStore.ProjectionEntry> snapshot) {
        for (int from = 0; from < snapshot.size(); from += REDIS_BATCH_SIZE) {
            int to = Math.min(from + REDIS_BATCH_SIZE, snapshot.size());
            redisStore.appendRebuildBatch(scope, generation, snapshot.subList(from, to));
        }
    }

    private void awaitWorkersIdle(LeaderboardScope scope) {
        long deadline = System.nanoTime() + WORKER_DRAIN_TIMEOUT.toNanos();
        while (outboxService.countProcessingForScope(scope) > 0) {
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("Timed out waiting for projection workers to drain scope " + scope.registryValue());
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for projection workers", exception);
            }
        }
    }

    public record RebuildResult(LeaderboardScope scope, String generation, int entries) {
    }
}
