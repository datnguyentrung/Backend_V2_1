package com.dat.ai_receptionist_web.dto.Operation;

import com.dat.ai_receptionist_web.enums.Operation.AttendanceStatus;

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
