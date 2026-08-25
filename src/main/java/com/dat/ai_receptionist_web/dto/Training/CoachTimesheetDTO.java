package com.dat.ai_receptionist_web.dto.Training;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public final class CoachTimesheetDTO {
    private CoachTimesheetDTO() {
    }

    public record CreateRequest(@NotNull UUID coachAssignmentId, @NotNull UUID classSessionId, @NotNull LocalTime checkInTime, @NotNull LocalTime checkOutTime, @NotNull String note) {
    }

    public record UpdateRequest(@NotNull UUID coachAssignmentId, @NotNull UUID classSessionId, @NotNull LocalTime checkInTime, @NotNull LocalTime checkOutTime, @NotNull String note) {
    }

    public record Response(UUID coachTimesheetId, UUID coachAssignmentId, UUID classSessionId, LocalTime checkInTime, LocalTime checkOutTime, String note, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
