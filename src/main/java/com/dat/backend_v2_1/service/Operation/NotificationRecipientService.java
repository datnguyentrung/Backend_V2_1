package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Operation.Notification;
import com.dat.backend_v2_1.domain.Operation.NotificationRecipient;
import com.dat.backend_v2_1.dto.Operation.NotificationDTO;
import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Operation.NotificationRecipientStatus;
import com.dat.backend_v2_1.enums.Operation.NotificationType;
import com.dat.backend_v2_1.repository.Operation.NotificationRecipientRepository;
import com.dat.backend_v2_1.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationRecipientService {

    private final NotificationRecipientRepository notificationRecipientRepository;

    @Transactional(readOnly = true)
    public NotificationDTO.RecipientListResponse filterForCurrentUser(
            Boolean read,
            NotificationRecipientStatus status,
            NotificationType type,
            LocalDateTime fromCreatedAt,
            LocalDateTime toCreatedAt,
            LocalDateTime fromReadAt,
            LocalDateTime toReadAt,
            String search,
            Pageable pageable
    ) {
        validateDateRange(fromCreatedAt, toCreatedAt);
        validateDateRange(fromReadAt, toReadAt);

        UUID userId = currentUserId();
        Page<NotificationRecipient> page = notificationRecipientRepository.filterForUser(
                userId,
                read,
                status,
                type,
                fromCreatedAt,
                toCreatedAt,
                fromReadAt,
                toReadAt,
                normalizeSearch(search),
                pageable
        );

        return NotificationDTO.RecipientListResponse.builder()
                .unreadCount(notificationRecipientRepository.countByRecipientUser_UserIdAndReadFalse(userId))
                .notifications(PageResponse.of(page, this::toResponse))
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationDTO.RecipientResponse getMine(UUID notificationRecipientId) {
        UUID userId = currentUserId();
        return notificationRecipientRepository
                .findByNotificationRecipientIdAndRecipientUser_UserId(notificationRecipientId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new AccessDeniedException("Notification is not allowed"));
    }

    @Transactional
    public void markRead(UUID notificationRecipientId) {
        notificationRecipientRepository.markRead(notificationRecipientId, currentUserId(), LocalDateTime.now());
    }

    private NotificationDTO.RecipientResponse toResponse(NotificationRecipient recipient) {
        Notification notification = recipient.getNotification();
        return NotificationDTO.RecipientResponse.builder()
                .notificationRecipientId(recipient.getNotificationRecipientId())
                .notificationId(notification.getNotificationId())
                .recipientUserId(recipient.getRecipientUser().getUserId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .notificationType(notification.getNotificationType())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .payload(notification.getPayload())
                .read(recipient.isRead())
                .readAt(recipient.getReadAt())
                .deliveredAt(recipient.getDeliveredAt())
                .recipientStatus(recipient.getRecipientStatus())
                .createdAt(recipient.getCreatedAt())
                .updatedAt(recipient.getUpdatedAt())
                .build();
    }

    private UUID currentUserId() {
        return SecurityUtil.getCurrentUserId()
                .map(UUID::fromString)
                .orElseThrow(() -> new AccessDeniedException("Missing user id"));
    }

    private String normalizeSearch(String search) {
        return search == null ? null : search.trim();
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid date range");
        }
    }
}
