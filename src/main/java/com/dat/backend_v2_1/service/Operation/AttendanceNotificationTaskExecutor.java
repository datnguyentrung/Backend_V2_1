package com.dat.backend_v2_1.service.Operation;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class AttendanceNotificationTaskExecutor {

    private final ExecutorService executorService;

    public AttendanceNotificationTaskExecutor() {
        this(createExecutor());
    }

    AttendanceNotificationTaskExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void submit(UUID attendanceId, Runnable task) {
        try {
            executorService.execute(() -> runTask(attendanceId, task));
        } catch (RejectedExecutionException e) {
            log.error("Attendance notification queue rejected task for attendanceId={}", attendanceId, e);
        }
    }

    private void runTask(UUID attendanceId, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("Failed to process attendance notification for attendanceId={}", attendanceId, e);
        }
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdown();
    }

    private static ExecutorService createExecutor() {
        return new ThreadPoolExecutor(
                1,
                2,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50),
                new NamedThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "attendance-notification-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
