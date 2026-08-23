package com.dat.ai_receptionist_web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    private boolean trustForwardedHeaders;
    private List<Policy> policies = new ArrayList<>();

    @Getter
    @Setter
    public static class Policy {
        private String name;
        private String path;
        private String method;
        private int limit;
        private Duration window;
        private Subject subject = Subject.AUTO;
    }

    public enum Subject {
        IP, AUTHENTICATED, AUTO
    }
}
