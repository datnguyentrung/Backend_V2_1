package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.enums.Operation.AttendanceStatus;
import com.dat.backend_v2_1.enums.Operation.EvaluationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface CompletedSessionAttendanceNotificationRow {
    UUID getAttendanceId();

    UUID getSessionId();

    UUID getStudentPersonId();

    String getStudentName();

    AttendanceStatus getAttendanceStatus();

    LocalDateTime getCheckInTime();

    LocalDateTime getCreatedAt();

    EvaluationStatus getEvaluationStatus();

    String getNote();

    String getScheduleId();

    LocalDate getSessionDate();
}
