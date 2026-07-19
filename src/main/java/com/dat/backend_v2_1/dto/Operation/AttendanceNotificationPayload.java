package com.dat.backend_v2_1.dto.Operation;

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
