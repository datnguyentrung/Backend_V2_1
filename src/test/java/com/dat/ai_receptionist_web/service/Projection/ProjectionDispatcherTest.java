package com.dat.ai_receptionist_web.service.Projection;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.service.Projection.handler.ProjectionHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectionDispatcherTest {
    @Test
    void dispatchesToMatchingHandler() {
        AtomicBoolean processed = new AtomicBoolean(false);
        ProjectionHandler handler = new ProjectionHandler() {
            @Override
            public ProjectionType supports() {
                return ProjectionType.LEADERBOARD_CONDUCT;
            }

            @Override
            public void process(ProjectionJob job) {
                processed.set(true);
            }
        };
        ProjectionDispatcher dispatcher = new ProjectionDispatcher(List.of(handler));

        dispatcher.process(new ProjectionJob(
                1L, ProjectionType.LEADERBOARD_CONDUCT, "key", "student",
                2026, 3, null, "{}", 1L, 0
        ));

        assertThat(processed).isTrue();
    }
}
