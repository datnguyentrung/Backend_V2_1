package com.dat.ai_receptionist_web.dto.Operation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AttendanceNotificationPayload(
        UUID notificationId,
        List<String> tokens,
        String title,
        String body,
        Map<String, String> data
) {
}
