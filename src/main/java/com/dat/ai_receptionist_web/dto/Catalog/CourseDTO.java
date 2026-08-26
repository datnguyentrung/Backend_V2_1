package com.dat.ai_receptionist_web.dto.Catalog;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import java.util.UUID;

public final class CourseDTO {
    private CourseDTO() {
    }

    public record CreateRequest(
            @NotNull(message = "Class schedule ID is required")
            UUID classScheduleId,

            @NotBlank(message = "Name is required")
            String name,

            int capacity,

            @NotNull(message = "Status is required")
            CourseStatus status
    ) {
    }

    public record UpdateRequest(
            @NotNull(message = "Class schedule ID is required")
            UUID classScheduleId,

            @NotNull(message = "Name is required")
            String name,

            int capacity,
            @NotNull(message = "Status is required") CourseStatus status) {
    }

    public record Response(
            UUID courseId,
            UUID classScheduleId,
            String name,
            int capacity,
            CourseStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
