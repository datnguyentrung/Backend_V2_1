package com.dat.ai_receptionist_web.dto.Training;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Operation.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Operation.EvaluationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public final class StudentAttendanceDTO {
    private StudentAttendanceDTO() {
    }

    public record CreateRequest(@NotNull UUID classSessionId, @NotNull UUID studentEnrollmentId, @NotNull UUID evaluatedByCoachId, @NotNull LocalDateTime checkInTime, @NotNull AttendanceStatus attendanceStatus, @NotNull EvaluationStatus evaluationStatus, @NotNull String note) {
    }

    public record UpdateRequest(@NotNull UUID classSessionId, @NotNull UUID studentEnrollmentId, @NotNull UUID evaluatedByCoachId, @NotNull LocalDateTime checkInTime, @NotNull AttendanceStatus attendanceStatus, @NotNull EvaluationStatus evaluationStatus, @NotNull String note) {
    }

    public record Response(UUID studentAttendanceId, UUID classSessionId, UUID studentEnrollmentId, UUID evaluatedByCoachId, LocalDateTime checkInTime, AttendanceStatus attendanceStatus, EvaluationStatus evaluationStatus, String note, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
