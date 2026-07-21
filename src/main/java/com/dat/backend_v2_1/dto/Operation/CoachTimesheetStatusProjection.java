package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.enums.Operation.CoachTimesheetStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CoachTimesheetStatusProjection {
    UUID getAssignmentId();

    CoachTimesheetStatus getStatus();

    LocalDateTime getCheckInTime();

    LocalDateTime getCheckOutTime();
}
