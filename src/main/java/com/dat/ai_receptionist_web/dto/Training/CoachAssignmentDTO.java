package com.dat.ai_receptionist_web.dto.Training;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Operation.CoachAssignmentStatus;
import java.time.LocalDate;
import java.util.UUID;

public final class CoachAssignmentDTO {
    private CoachAssignmentDTO() {
    }

    public record CreateRequest(@NotNull UUID coachId, @NotNull UUID courseId, @NotNull LocalDate assignedDate, @NotNull LocalDate endDate, @NotNull CoachAssignmentStatus coachAssignmentStatus, @NotNull String note) {
    }

    public record UpdateRequest(@NotNull UUID coachId, @NotNull UUID courseId, @NotNull LocalDate assignedDate, @NotNull LocalDate endDate, @NotNull CoachAssignmentStatus coachAssignmentStatus, @NotNull String note) {
    }

    public record Response(UUID coachAssignmentId, UUID coachId, UUID courseId, LocalDate assignedDate, LocalDate endDate, CoachAssignmentStatus coachAssignmentStatus, String note, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
