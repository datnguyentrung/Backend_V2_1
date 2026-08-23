package com.dat.ai_receptionist_web.service.Projection;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectionOutboxWorkerTest {
    @Test
    void releasesNewRevisionWhenClaimedRevisionWasSuperseded() {
        ProjectionOutboxService outboxService = mock(ProjectionOutboxService.class);
        ProjectionDispatcher dispatcher = mock(ProjectionDispatcher.class);
        ProjectionRetryPolicy retryPolicy = new ProjectionRetryPolicy();
        ProjectionJob job = new ProjectionJob(
                7L, ProjectionType.LEADERBOARD_CONDUCT, "conduct-key", "VQ_A",
                2026, 3, null, "{}", 5L, 0
        );
        when(outboxService.claimReadyJobs(anyInt(), anyString())).thenReturn(List.of(job));
        when(outboxService.ack(eq(job.id()), eq(job.revision()), anyString())).thenReturn(false);
        when(outboxService.releaseSuperseded(eq(job.id()), eq(job.revision()), anyString())).thenReturn(true);

        ProjectionOutboxWorker worker = new ProjectionOutboxWorker(
                outboxService, dispatcher, retryPolicy, new SimpleMeterRegistry()
        );
        ReflectionTestUtils.setField(worker, "batchSize", 20);

        worker.poll();

        verify(dispatcher).process(job);
        verify(outboxService).ack(eq(job.id()), eq(job.revision()), anyString());
        verify(outboxService).releaseSuperseded(eq(job.id()), eq(job.revision()), anyString());
    }
}
