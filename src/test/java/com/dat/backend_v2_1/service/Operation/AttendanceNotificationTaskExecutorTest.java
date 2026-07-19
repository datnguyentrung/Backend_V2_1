package com.dat.backend_v2_1.service.Operation;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AttendanceNotificationTaskExecutorTest {

    @Test
    void executorRejectionIsCaughtAndDoesNotRunOnCallerThread() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy()
        );
        executor.shutdownNow();

        AttendanceNotificationTaskExecutor taskExecutor = new AttendanceNotificationTaskExecutor(executor);
        AtomicBoolean ran = new AtomicBoolean(false);

        assertDoesNotThrow(() -> taskExecutor.submit(UUID.randomUUID(), () -> ran.set(true)));
        assertFalse(ran.get());
    }
}
