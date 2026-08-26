package com.dat.ai_receptionist_web.service.Projection;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;

public record ProjectionJob(
        long id,
        ProjectionType projectionType,
        String projectionKey,
        String aggregateKey,
        Integer year,
        Integer quarter,
        ScheduleLevel scheduleLevel,
        String payload,
        long revision,
        int retryCount
) {
}


