package com.dat.ai_receptionist_web.service.Projection;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ProjectionRetryPolicy {
    private static final Duration[] DELAYS = {
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            Duration.ofSeconds(15),
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15)
    };

    /**
     * Tác dụng: Thực hiện logic nextDelay của lớp hiện tại.
     * Input: Nhận int currentRetryCount từ caller hoặc request.
     * Output: Trả về giá trị Duration biểu thị kết quả tính toán hoặc số lượng.
     */
    public Duration nextDelay(int currentRetryCount) {
        Duration base = DELAYS[Math.min(currentRetryCount, DELAYS.length - 1)];
        double jitter = ThreadLocalRandom.current().nextDouble(0.8, 1.2);
        return Duration.ofMillis(Math.max(250L, Math.round(base.toMillis() * jitter)));
    }
}


