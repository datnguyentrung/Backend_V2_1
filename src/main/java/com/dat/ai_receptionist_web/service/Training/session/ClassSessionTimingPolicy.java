package com.dat.ai_receptionist_web.service.Training.session;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Logic thời gian thuần cho class session. DB bắt buộc endTime > startTime,
 * nên không xử lý qua đêm.
 */
public final class ClassSessionTimingPolicy {

    private ClassSessionTimingPolicy() {
    }

    public static LocalDateTime attendanceCloseTime(
            LocalDate sessionDate, LocalTime startTime, LocalTime endTime) {
        if (sessionDate == null || startTime == null || endTime == null) {
            return null;
        }
        if (!endTime.isAfter(startTime)) {
            return null;
        }
        LocalDateTime start = LocalDateTime.of(sessionDate, startTime);
        LocalDateTime end = LocalDateTime.of(sessionDate, endTime);
        return start.plus(Duration.between(start, end).dividedBy(2));
    }
}
