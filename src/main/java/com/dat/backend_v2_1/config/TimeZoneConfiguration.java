package com.dat.backend_v2_1.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeZoneConfiguration {

    private static final String VIETNAM_TIMEZONE = "Asia/Ho_Chi_Minh";

    @PostConstruct
    public void init() {
        // Đặt timezone mặc định cho JVM
        TimeZone.setDefault(TimeZone.getTimeZone(VIETNAM_TIMEZONE));
        System.setProperty("user.timezone", VIETNAM_TIMEZONE);
    }

    @Bean
    public ZoneId defaultZoneId() {
        return ZoneId.of(VIETNAM_TIMEZONE);
    }
}
