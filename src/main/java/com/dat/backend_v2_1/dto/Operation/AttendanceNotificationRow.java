package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AttendanceNotificationRow {
    UUID getAttendanceId();

    UUID getStudentPersonId();

    String getStudentName();

    AttendanceStatus getAttendanceStatus();

    LocalDateTime getCheckInTime();

    LocalDateTime getCreatedAt();

    String getScheduleId();

    String getCoachName();
}
