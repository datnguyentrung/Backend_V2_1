package com.dat.ai_receptionist_web.dto.Operation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface ClassSessionReportSessionRow {
    UUID getSessionId();

    LocalDate getSessionDate();

    String getClassScheduleId();

    String getBranchName();

    LocalTime getStartTime();

    LocalTime getEndTime();
}
