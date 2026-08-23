package com.dat.ai_receptionist_web.service.Projection;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectionRetryPolicyTest {
    private final ProjectionRetryPolicy policy = new ProjectionRetryPolicy();

    @Test
    void firstRetryUsesOneSecondBackoffWithJitter() {
        Duration delay = policy.nextDelay(0);

        assertThat(delay).isBetween(Duration.ofMillis(800), Duration.ofMillis(1200));
    }

    @Test
    void retriesAreCappedAtFifteenMinutesBeforeJitter() {
        Duration delay = policy.nextDelay(100);

        assertThat(delay).isBetween(Duration.ofMinutes(12), Duration.ofMinutes(18));
    }
}
