package com.dat.ai_receptionist_web.listener;

import com.dat.ai_receptionist_web.event.FitnessLeaderboardChangedEvent;
import com.dat.ai_receptionist_web.event.ScoreRecalculateEvent;
import com.dat.ai_receptionist_web.event.StudentLeaderboardChangedEvent;
import com.dat.ai_receptionist_web.service.Report.LeaderboardProjectionUpdater;
import com.dat.ai_receptionist_web.service.Report.LeaderboardRedisStore;
import com.dat.ai_receptionist_web.service.Report.LeaderboardScope;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardProjectionEventListener {
    private static final Duration[] RETRY_DELAYS = {
            Duration.ZERO,
            Duration.ofMillis(100),
            Duration.ofMillis(250)
    };

    private final LeaderboardProjectionUpdater projectionUpdater;
    private final LeaderboardRedisStore redisStore;
    private final MeterRegistry meterRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuarterScoreChanged(ScoreRecalculateEvent event) {
        String scope = LeaderboardScope.quarter(event.getYear(), event.getQuarter()).registryValue();
        runWithRetry("quarter", event.getStudentCode(), scope, () -> projectionUpdater.refreshQuarter(
                event.getStudentCode(), event.getYear(), event.getQuarter()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFitnessRecordChanged(FitnessLeaderboardChangedEvent event) {
        for (FitnessLeaderboardChangedEvent.Scope scope : event.affectedScopes()) {
            String scopeName = LeaderboardScope.fitness(scope.year(), scope.quarter(), scope.skillLevel()).registryValue();
            runWithRetry("fitness", event.studentCode(), scopeName, () -> projectionUpdater.refreshFitness(
                    event.studentCode(), scope.year(), scope.quarter(), scope.skillLevel()
            ));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudentChanged(StudentLeaderboardChangedEvent event) {
        AtomicReference<List<LeaderboardScope>> scopes = new AtomicReference<>();
        runWithRetry(
                "metadata_registry", event.studentCode(), "registry",
                () -> scopes.set(redisStore.registeredScopes())
        );
        if (scopes.get() == null) {
            return;
        }
        for (LeaderboardScope scope : scopes.get()) {
            runWithRetry("student", event.studentCode(), scope.registryValue(), () -> {
                if (!event.membershipChanged()) {
                    projectionUpdater.refreshMember(event.studentCode(), scope);
                } else if (scope.type() == LeaderboardScope.Type.QUARTER) {
                    projectionUpdater.refreshQuarter(event.studentCode(), scope.year(), scope.quarter());
                } else {
                    projectionUpdater.refreshFitness(
                            event.studentCode(), scope.year(), scope.quarter(), scope.skillLevel()
                    );
                }
            });
        }
    }

    private void runWithRetry(String type, String studentCode, String scope, Runnable operation) {
        Counter success = meterRegistry.counter("leaderboard.update", "type", type, "result", "success");
        Counter failure = meterRegistry.counter("leaderboard.update", "type", type, "result", "failure");
        RuntimeException lastFailure = null;

        for (int attempt = 0; attempt < RETRY_DELAYS.length; attempt++) {
            try {
                sleep(RETRY_DELAYS[attempt]);
                operation.run();
                success.increment();
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                log.warn("LEADERBOARD_UPDATE_RETRY type={} scope={} studentCode={} attempt={} result=failed",
                        type, scope, studentCode, attempt + 1, exception);
            }
        }

        failure.increment();
        meterRegistry.counter("leaderboard.update.retry.exhausted", "type", type).increment();
        log.error("LEADERBOARD_UPDATE_EXHAUSTED type={} scope={} studentCode={} attempts={}",
                type, scope, studentCode, RETRY_DELAYS.length, lastFailure);
    }

    private void sleep(Duration delay) {
        if (delay.isZero()) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Leaderboard retry interrupted", exception);
        }
    }
}
