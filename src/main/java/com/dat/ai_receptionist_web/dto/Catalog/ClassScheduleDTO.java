package com.dat.ai_receptionist_web.dto.Catalog;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLocation;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import java.time.LocalTime;
import java.util.UUID;

public final class ClassScheduleDTO {
    private ClassScheduleDTO() {
    }

    public record CreateRequest(@NotNull Long branchId, @NotNull Weekday weekday, @NotNull ScheduleLevel level, @NotNull ScheduleLocation location, @NotNull ScheduleStatus status, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {
    }

    public record UpdateRequest(@NotNull Long branchId, @NotNull Weekday weekday, @NotNull ScheduleLevel level, @NotNull ScheduleLocation location, @NotNull ScheduleStatus status, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {
    }

    public record Response(UUID scheduleId, Long branchId, Weekday weekday, ScheduleLevel level, ScheduleLocation location, ScheduleStatus status, LocalTime startTime, LocalTime endTime) {
    }
}
