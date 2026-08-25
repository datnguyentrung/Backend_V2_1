package com.dat.ai_receptionist_web.dto.Skill;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public final class FitnessRecordDTO {
    private FitnessRecordDTO() {
    }

    public record CreateRequest(@NotNull UUID studentId, @NotNull Long fitnessId, @NotNull UUID recordedByCoachId, @NotNull LocalDate recordDate, int duration) {
    }

    public record UpdateRequest(@NotNull UUID studentId, @NotNull Long fitnessId, @NotNull UUID recordedByCoachId, @NotNull LocalDate recordDate, int duration) {
    }

    public record Response(Long fitnessRecordId, UUID studentId, Long fitnessId, UUID recordedByCoachId, LocalDate recordDate, int duration, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
