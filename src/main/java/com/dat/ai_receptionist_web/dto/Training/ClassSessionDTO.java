package com.dat.ai_receptionist_web.dto.Training;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class ClassSessionDTO {
    private ClassSessionDTO() {
    }

    public record CreateRequest(@NotNull UUID courseId, @NotNull LocalDate sessionDate, @NotNull SessionStatus status, boolean attendanceClosed, @NotNull LocalTime startTime, @NotNull LocalTime endTime, @NotNull String note) {
    }

    public record UpdateRequest(@NotNull UUID courseId, @NotNull LocalDate sessionDate, @NotNull SessionStatus status, boolean attendanceClosed, @NotNull LocalTime startTime, @NotNull LocalTime endTime, @NotNull String note) {
    }

    public record Response(UUID classSessionId, UUID courseId, LocalDate sessionDate, SessionStatus status, boolean attendanceClosed, LocalTime startTime, LocalTime endTime, String note) {
    }
}
