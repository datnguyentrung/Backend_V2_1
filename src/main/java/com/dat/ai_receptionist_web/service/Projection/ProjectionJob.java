package com.dat.ai_receptionist_web.service.Projection;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.enums.Skill.SkillLevel;

public record ProjectionJob(
        long id,
        ProjectionType projectionType,
        String projectionKey,
        String aggregateKey,
        Integer year,
        Integer quarter,
        SkillLevel skillLevel,
        String payload,
        long revision,
        int retryCount
) {
}
