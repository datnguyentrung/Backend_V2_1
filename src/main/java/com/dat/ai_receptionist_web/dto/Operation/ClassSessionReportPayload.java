package com.dat.ai_receptionist_web.dto.Operation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ClassSessionReportPayload(
        UUID sessionId,
        String classScheduleId,
        String title,
        String body,
        Map<String, String> data,
        Set<UUID> coachPersonIds
) {
}
