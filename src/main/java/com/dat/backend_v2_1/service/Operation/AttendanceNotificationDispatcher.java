package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.dto.Operation.AttendanceNotificationPayload;
import com.dat.backend_v2_1.dto.Operation.FirebaseMulticastResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceNotificationDispatcher {

    private final NotificationPreparationService notificationPreparationService;
    private final FirebaseNotificationSender firebaseNotificationSender;
    private final NotificationStatusService notificationStatusService;

    public void dispatch(UUID attendanceId) {
        Optional<AttendanceNotificationPayload> payloadOptional =
                notificationPreparationService.preparePendingNotification(attendanceId);
        if (payloadOptional.isEmpty()) {
            return;
        }

        AttendanceNotificationPayload payload = payloadOptional.get();
        FirebaseMulticastResult result = firebaseNotificationSender.send(payload);
        if (!result.attempted()) {
            log.info("Notification send was not attempted for attendanceId={}, notificationId={}",
                    attendanceId, payload.notificationId());
            return;
        }

        if (result.hasSuccess()) {
            notificationStatusService.markSent(payload.notificationId());
            log.info("Attendance notification sent for attendanceId={}, notificationId={}, success={}/{}",
                    attendanceId, payload.notificationId(), result.successCount(), result.attemptedCount());
            return;
        }

        notificationStatusService.markFailed(payload.notificationId());
        log.warn("Attendance notification failed for attendanceId={}, notificationId={}, failures={}",
                attendanceId, payload.notificationId(), result.failureCount());
    }
}
