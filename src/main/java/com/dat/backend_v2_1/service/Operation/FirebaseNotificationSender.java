package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.dto.Operation.AttendanceNotificationPayload;
import com.dat.backend_v2_1.dto.Operation.FirebaseMulticastResult;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.WebpushConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FirebaseNotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    public FirebaseNotificationSender(ObjectProvider<FirebaseMessaging> firebaseMessagingProvider) {
        this.firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
    }

    public FirebaseMulticastResult send(AttendanceNotificationPayload payload) {
        if (payload.tokens() == null || payload.tokens().isEmpty()) {
            log.info("Skipping Firebase send for notificationId={} because token list is empty", payload.notificationId());
            return FirebaseMulticastResult.skipped();
        }

        if (firebaseMessaging == null) {
            log.debug("FirebaseMessaging is not configured; skipping notificationId={}", payload.notificationId());
            return FirebaseMulticastResult.skipped();
        }

        try {
            Map<String, String> firebasePayload = new HashMap<>();
            if (payload.data() != null) {
                firebasePayload.putAll(payload.data());
            }
            firebasePayload.put("title", payload.title());
            firebasePayload.put("body", payload.body());
            firebasePayload.put("tag", "attendance-notification");

            WebpushConfig webpushConfig = WebpushConfig.builder()
                    .putHeader("Urgency", "high")
                    .putHeader("TTL", "300")
                    .build();

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(payload.tokens())
                    .putAllData(firebasePayload)
                    .setWebpushConfig(webpushConfig)
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            return new FirebaseMulticastResult(
                    payload.tokens().size(),
                    response.getSuccessCount(),
                    response.getFailureCount()
            );
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send Firebase notificationId={}", payload.notificationId(), e);
            return new FirebaseMulticastResult(payload.tokens().size(), 0, payload.tokens().size());
        }
    }
}
