package com.dat.ai_receptionist_web.service.Notification;
import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.enums.Operation.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.service.Security.AuthSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {
    private final NotificationRecipientRepository recipientRepository;
    private final AuthSessionService authSessionService;
    private final FirebaseNotificationSender sender;
    private final PlatformTransactionManager transactionManager;
    public void deliver(UUID notificationId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<Delivery> deliveries = transaction.execute(status ->
                recipientRepository.findDeliveryRows(notificationId).stream()
                        .map(row -> new Delivery(row.getNotificationRecipientId(),
                                row.getRecipientUser().getUserId(), row.getNotification().getTitle(),
                                row.getNotification().getBody(), row.getNotification().getPayload())).toList());
        if (deliveries == null) return;
        for (Delivery delivery : deliveries) {
            boolean sent = sender.send(authSessionService.fcmTokensForUser(delivery.userId()),
                    delivery.title(), delivery.body(), delivery.payload());
            transaction.executeWithoutResult(status -> {
                NotificationRecipient recipient = recipientRepository.findById(delivery.recipientId()).orElseThrow();
                recipient.setNotificationRecipientStatus(sent
                        ? NotificationRecipientStatus.SENT : NotificationRecipientStatus.FAILED);
                if (sent) recipient.setDeliveredAt(LocalDateTime.now());
            });
        }
    }
    private record Delivery(UUID recipientId, UUID userId, String title, String body, String payload) {}
}
