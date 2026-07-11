package com.dat.backend_v2_1.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "attendance")
public class AttendanceProperties {
    @Value("${ATTENDANCE_GRACE_PERIOD_MINUTES:30}")
    private int earlyCheckInMinutes = 30;

    private int gracePeriodMinutes = 15;

    private int lateCheckInLimitMinutes = 30;
}
