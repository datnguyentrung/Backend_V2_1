package com.dat.ai_receptionist_web.service.Security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> faceCheckInBuckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ipAddress) {
        return loginBuckets.computeIfAbsent(ipAddress, this::newLoginBucket);
    }

    public Bucket resolveFaceCheckInBucket(String ipAddress) {
        return faceCheckInBuckets.computeIfAbsent(ipAddress, this::newFaceCheckInBucket);
    }

    private Bucket newLoginBucket(String ipAddress) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(1000)
                .refillIntervally(5, Duration.ofMinutes(5))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket newFaceCheckInBucket(String ipAddress) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(300)
                .refillIntervally(300, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
