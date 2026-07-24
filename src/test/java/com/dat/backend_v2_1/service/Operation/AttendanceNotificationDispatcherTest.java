package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.dto.Operation.AttendanceNotificationPayload;
import com.dat.ai_receptionist_web.dto.Operation.FirebaseMulticastResult;
import com.dat.ai_receptionist_web.service.Operation.AttendanceNotificationDispatcher;
import com.dat.ai_receptionist_web.service.Operation.FirebaseNotificationSender;
import com.dat.ai_receptionist_web.service.Operation.NotificationPreparationService;
import com.dat.ai_receptionist_web.service.Operation.NotificationStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceNotificationDispatcherTest {

    @Mock
    private NotificationPreparationService notificationPreparationService;

    @Mock
    private FirebaseNotificationSender firebaseNotificationSender;

    @Mock
    private NotificationStatusService notificationStatusService;

    @InjectMocks
    private AttendanceNotificationDispatcher dispatcher;

    @Test
    void partialFirebaseSuccessMarksNotificationSent() {
        UUID attendanceId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        AttendanceNotificationPayload payload = new AttendanceNotificationPayload(
                notificationId,
                List.of("token-1", "token-2"),
                "title",
                "body",
                Map.of()
        );

        when(notificationPreparationService.preparePendingNotification(attendanceId)).thenReturn(Optional.of(payload));
        when(firebaseNotificationSender.send(payload)).thenReturn(new FirebaseMulticastResult(2, 1, 1));

        dispatcher.dispatch(attendanceId);

        verify(notificationStatusService).markSent(notificationId);
        verify(notificationStatusService, never()).markFailed(notificationId);
    }

    @Test
    void allAttemptedFirebaseTokensFailMarksNotificationFailed() {
        UUID attendanceId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        AttendanceNotificationPayload payload = new AttendanceNotificationPayload(
                notificationId,
                List.of("token-1", "token-2"),
                "title",
                "body",
                Map.of()
        );

        when(notificationPreparationService.preparePendingNotification(attendanceId)).thenReturn(Optional.of(payload));
        when(firebaseNotificationSender.send(payload)).thenReturn(new FirebaseMulticastResult(2, 0, 2));

        dispatcher.dispatch(attendanceId);

        verify(notificationStatusService).markFailed(notificationId);
        verify(notificationStatusService, never()).markSent(notificationId);
    }

    @Test
    void noTokenSkipDoesNotMarkFalseSent() {
        UUID attendanceId = UUID.randomUUID();

        when(notificationPreparationService.preparePendingNotification(attendanceId)).thenReturn(Optional.empty());

        dispatcher.dispatch(attendanceId);

        verify(notificationStatusService, never()).markSent(org.mockito.ArgumentMatchers.any());
        verify(notificationStatusService, never()).markFailed(org.mockito.ArgumentMatchers.any());
    }
}
