package com.dat.ai_receptionist_web.service.Security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitingServiceTest {
    @Test
    void fallsBackToLocalAtomicCounterWhenRedisIsUnavailable() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("offline"));
        RateLimitingService service = new RateLimitingService(redis);

        assertThat(service.allow("login", "ip:127.0.0.1", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(service.allow("login", "ip:127.0.0.1", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(service.allow("login", "ip:127.0.0.1", 2, Duration.ofMinutes(1))).isFalse();
    }
}
