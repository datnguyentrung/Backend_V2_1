package com.dat.ai_receptionist_web.dto.Catalog;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import java.util.UUID;

public final class CourseDTO {
    private CourseDTO() {
    }

    public record CreateRequest(@NotNull UUID classScheduleId, int capacity, @NotNull CourseStatus status) {
    }

    public record UpdateRequest(@NotNull UUID classScheduleId, int capacity, @NotNull CourseStatus status) {
    }

    public record Response(UUID courseId, UUID classScheduleId, int capacity, CourseStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
