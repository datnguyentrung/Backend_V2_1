package com.dat.backend_v2_1.service.Security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // Lưu trữ bucket theo địa chỉ IP
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ipAddress) {
        return cache.computeIfAbsent(ipAddress, this::newBucket);
    }

    private Bucket newBucket(String ipAddress) {
        // Cú pháp MỚI NHẤT: Không dùng Refill và classic nữa
        Bandwidth limit = Bandwidth.builder()
                .capacity(5) // Thể tích xô: Chứa tối đa 5 lần thử
                .refillIntervally(5, Duration.ofMinutes(15)) // Tốc độ hồi phục: Cứ 15 phút bơm lại 5 giọt
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
