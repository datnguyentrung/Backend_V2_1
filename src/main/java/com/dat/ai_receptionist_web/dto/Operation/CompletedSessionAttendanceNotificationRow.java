package com.dat.ai_receptionist_web.dto.Operation;

import com.dat.ai_receptionist_web.enums.Operation.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Operation.EvaluationStatus;

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
