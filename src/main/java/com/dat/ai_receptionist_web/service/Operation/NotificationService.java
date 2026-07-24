package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.domain.Operation.NotificationRecipient;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Operation.NotificationDTO;
import com.dat.ai_receptionist_web.enums.Operation.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.enums.Operation.NotificationType;
import com.dat.ai_receptionist_web.repository.Operation.NotificationRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AttendanceNotificationTaskExecutor notificationExecutor;

    public NotificationService(
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            AttendanceNotificationTaskExecutor notificationExecutor
    ) {
        this.firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.notificationExecutor = notificationExecutor;
    }

    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.debug("FirebaseMessaging is not configured; skipping notification send.");
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notification);

            if (data != null) {
                messageBuilder.putAllData(data);
            }

            String response = firebaseMessaging.send(messageBuilder.build());
            log.info("Firebase notification sent successfully: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send Firebase notification", e);
        }
    }

    public void sendMulticastNotification(List<String> tokens, String title, String body) {
        sendMulticastNotification(tokens, title, body, null);
    }

    public void sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        enqueueFirebaseAfterCommit(null, tokens, title, body, data);
    }

    @Transactional
    public NotificationDTO.NotificationResponse sendMulticastNotification(
            List<String> tokens,
            List<UUID> recipientUserIds,
            String title,
            String body,
            NotificationType notificationType,
            String referenceType,
            String referenceId,
            Map<String, String> data
    ) {
        NotificationDTO.NotificationResponse savedNotification = saveNotification(
                title,
                body,
                notificationType,
                referenceType,
                referenceId,
                data,
                recipientUserIds
        );
        enqueueFirebaseAfterCommit(savedNotification.getNotificationId(), tokens, title, body, data);
        return savedNotification;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<NotificationDTO.NotificationResponse> createClassSessionReportIfAbsent(
            String title,
            String body,
            String referenceId,
            Map<String, String> data,
            List<UUID> recipientUserIds
    ) {
        if (notificationRepository.existsByNotificationTypeAndReferenceTypeAndReferenceId(
                NotificationType.CLASS_SESSION_REPORT,
                "CLASS_SESSION",
                referenceId
        )) {
            return Optional.empty();
        }

        List<UUID> distinctRecipientUserIds = recipientUserIds == null
                ? List.of()
                : recipientUserIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        return Optional.of(saveNotification(
                title,
                body,
                NotificationType.CLASS_SESSION_REPORT,
                "CLASS_SESSION",
                referenceId,
                data,
                distinctRecipientUserIds
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<NotificationDTO.NotificationResponse> createClassSessionAttendanceResultIfAbsent(
            String title,
            String body,
            String attendanceId,
            Map<String, String> data,
            List<UUID> recipientUserIds
    ) {
        if (notificationRepository.existsByNotificationTypeAndReferenceTypeAndReferenceId(
                NotificationType.ATTENDANCE,
                "CLASS_SESSION_ATTENDANCE_RESULT",
                attendanceId
        )) {
            return Optional.empty();
        }

        List<UUID> distinctRecipientUserIds = recipientUserIds == null
                ? List.of()
                : recipientUserIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        return Optional.of(saveNotification(
                title,
                body,
                NotificationType.ATTENDANCE,
                "CLASS_SESSION_ATTENDANCE_RESULT",
                attendanceId,
                data,
                distinctRecipientUserIds
        ));
    }

    @Transactional
    public NotificationDTO.NotificationResponse create(NotificationDTO.CreateRequest request) {
        return saveNotification(
                request.getTitle(),
                request.getBody(),
                request.getNotificationType(),
                request.getReferenceType(),
                request.getReferenceId(),
                request.getPayload(),
                request.getRecipientUserIds()
        );
    }

    @Transactional(readOnly = true)
    public NotificationDTO.NotificationResponse getDetail(UUID notificationId) {
        com.dat.ai_receptionist_web.domain.Operation.Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        return toResponse(notification);
    }

    private void sendFirebaseMulticast(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            log.warn("Empty token list; skipping multicast notification.");
            return;
        }

        if (firebaseMessaging == null) {
            log.debug("FirebaseMessaging is not configured; skipping multicast notification send.");
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();

            if (data != null) {
                payload.putAll(data);
            }

            payload.put("title", title);
            payload.put("body", body);
            payload.put("tag", "attendance-notification");

            WebpushConfig webpushConfig = WebpushConfig.builder()
                    .putHeader("Urgency", "high")
                    .putHeader("TTL", "300")
                    .build();

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .putAllData(payload)
                    .setWebpushConfig(webpushConfig)
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);

            log.info("Firebase multicast notification sent. Success: {}/{}",
                    response.getSuccessCount(), tokens.size());
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send Firebase multicast notification", e);
        }
    }

    private void enqueueFirebaseAfterCommit(
            UUID notificationId,
            List<String> tokens,
            String title,
            String body,
            Map<String, String> data
    ) {
        Runnable enqueue = () -> {
            notificationExecutor.submit(
                    notificationId,
                    () -> sendFirebaseMulticast(tokens, title, body, data)
            );
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue.run();
                }
            });
            return;
        }

        enqueue.run();
    }

    private NotificationDTO.NotificationResponse saveNotification(
            String title,
            String body,
            NotificationType notificationType,
            String referenceType,
            String referenceId,
            Map<String, String> data,
            List<UUID> recipientUserIds
    ) {
        return saveNotification(
                title,
                body,
                notificationType,
                referenceType,
                referenceId,
                toJson(data),
                recipientUserIds
        );
    }

    private NotificationDTO.NotificationResponse saveNotification(
            String title,
            String body,
            NotificationType notificationType,
            String referenceType,
            String referenceId,
            String payload,
            List<UUID> recipientUserIds
    ) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            throw new IllegalArgumentException("Recipient users must not be empty");
        }

        List<User> recipients = userRepository.findAllById(recipientUserIds);
        Set<UUID> foundUserIds = recipients.stream().map(User::getUserId).collect(Collectors.toSet());
        List<UUID> missingUserIds = recipientUserIds.stream()
                .filter(userId -> !foundUserIds.contains(userId))
                .toList();
        if (!missingUserIds.isEmpty()) {
//            throw new IllegalArgumentException("Recipient users not found: " + missingUserIds);
            // Không sao cả, chỉ báo là Phụ huynh chưa đăng ký tài khoản, vẫn lưu thông báo nhưng không gửi cho họ
            log.warn("Recipient users not found: {}", missingUserIds);
        }

        com.dat.ai_receptionist_web.domain.Operation.Notification notification =
                com.dat.ai_receptionist_web.domain.Operation.Notification.builder()
                        .title(title)
                        .body(body)
                        .notificationType(notificationType == null ? NotificationType.SYSTEM : notificationType)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .payload(payload)
                        .build();

        notification.getRecipients().addAll(recipients.stream()
                .map(user -> NotificationRecipient.builder()
                        .notification(notification)
                        .recipientUser(user)
                        .recipientStatus(NotificationRecipientStatus.PENDING)
                        .build())
                .toList());

        return toResponse(notificationRepository.save(notification));
    }

    private NotificationDTO.NotificationResponse toResponse(
            com.dat.ai_receptionist_web.domain.Operation.Notification notification
    ) {
        return NotificationDTO.NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .notificationType(notification.getNotificationType())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .payload(notification.getPayload())
                .createdAt(notification.getCreatedAt())
                .recipientCount(notification.getRecipients().size())
                .build();
    }

    private String toJson(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Notification payload is invalid", e);
        }
    }
}
