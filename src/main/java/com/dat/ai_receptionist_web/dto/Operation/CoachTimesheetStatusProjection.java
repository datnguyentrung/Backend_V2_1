package com.dat.ai_receptionist_web.dto.Operation;

import com.dat.ai_receptionist_web.enums.Operation.CoachTimesheetStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CoachTimesheetStatusProjection {
    UUID getAssignmentId();

    CoachTimesheetStatus getStatus();

    LocalDateTime getCheckInTime();

    LocalDateTime getCheckOutTime();
}
