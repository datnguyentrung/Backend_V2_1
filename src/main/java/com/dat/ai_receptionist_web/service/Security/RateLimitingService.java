package com.dat.ai_receptionist_web.service.Security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RateLimitingService {
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ConcurrentMap<String, LocalCounter> local = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong nextRedisWarningAt = new java.util.concurrent.atomic.AtomicLong();

    /**
     * Tác dụng: Thực hiện logic RateLimitingService của lớp hiện tại.
     * Input: Nhận StringRedisTemplate redis từ caller hoặc request.
     * Output: Khởi tạo instance của lớp với các phụ thuộc đầu vào.
     */
    public RateLimitingService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Tác dụng: Kiểm tra yêu cầu có được phép tiếp tục theo giới hạn hiện tại hay không.
     * Input: Nhận String policy, String subject, int limit, Duration window từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    public boolean allow(String policy, String subject, int limit, Duration window) {
        long windowMillis = window.toMillis();
        long bucket = System.currentTimeMillis() / windowMillis;
        String key = "rate-limit:" + policy + ":" + bucket + ":" + subject;
        try {
            Long count = redis.execute(INCREMENT, java.util.List.of(key), Long.toString(windowMillis));
            return count != null && count <= limit;
        } catch (RuntimeException redisFailure) {
            warnRedisFallback(redisFailure);
            return localAllow(key, limit, bucket);
        }
    }

    /**
     * Tác dụng: Thực hiện logic warnRedisFallback của lớp hiện tại.
     * Input: Nhận RuntimeException failure từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void warnRedisFallback(RuntimeException failure) {
        long now = System.currentTimeMillis();
        long next = nextRedisWarningAt.get();
        if (now >= next && nextRedisWarningAt.compareAndSet(next, now + 60_000)) {
            log.warn("Redis rate-limit unavailable; using instance-local fallback: {}",
                    failure.getClass().getSimpleName());
        }
    }

    /**
     * Tác dụng: Thực hiện logic localAllow của lớp hiện tại.
     * Input: Nhận String key, int limit, long bucket từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    private boolean localAllow(String key, int limit, long bucket) {
        if (local.size() > 10_000) {
            local.entrySet().removeIf(entry -> entry.getValue().bucket() < bucket - 1);
        }
        LocalCounter counter = local.compute(key, (ignored, current) ->
                current == null || current.bucket() != bucket
                        ? new LocalCounter(bucket, new AtomicInteger(1))
                        : new LocalCounter(bucket, new AtomicInteger(current.count().incrementAndGet())));
        return counter.count().get() <= limit;
    }

    private record LocalCounter(long bucket, AtomicInteger count) {
    }
}


