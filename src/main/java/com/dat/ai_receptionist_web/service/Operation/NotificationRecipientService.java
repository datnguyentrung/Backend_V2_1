package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.domain.Operation.Notification;
import com.dat.ai_receptionist_web.domain.Operation.NotificationRecipient;
import com.dat.ai_receptionist_web.dto.Operation.NotificationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Operation.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.enums.Operation.NotificationType;
import com.dat.ai_receptionist_web.repository.Operation.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import com.dat.ai_receptionist_web.util.error.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationRecipientService {

    private final NotificationRecipientRepository notificationRecipientRepository;
    private final ZoneId defaultZoneId;

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
        Page<NotificationRecipient> page = notificationRecipientRepository.findAll(
                buildFilterSpec(
                        userId,
                        read,
                        status,
                        type,
                        fromCreatedAt,
                        toCreatedAt,
                        fromReadAt,
                        toReadAt,
                        normalizeSearch(search)
                ),
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
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_RECIPIENT_NOT_FOUND));
    }

    @Transactional
    public void markRead(UUID notificationRecipientId) {
        UUID userId = currentUserId();
        int updated = notificationRecipientRepository.markRead(
                notificationRecipientId,
                userId,
                LocalDateTime.now(defaultZoneId)
        );
        if (updated == 0 && !notificationRecipientRepository
                .existsByNotificationRecipientIdAndRecipientUser_UserId(notificationRecipientId, userId)) {
            throw new AppException(ErrorCode.NOTIFICATION_RECIPIENT_NOT_FOUND);
        }
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
        return search == null || search.isBlank()
                ? null
                : search.trim().toLowerCase();
    }

    private Specification<NotificationRecipient> buildFilterSpec(
            UUID userId,
            Boolean read,
            NotificationRecipientStatus status,
            NotificationType type,
            LocalDateTime fromCreatedAt,
            LocalDateTime toCreatedAt,
            LocalDateTime fromReadAt,
            LocalDateTime toReadAt,
            String search
    ) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            Join<NotificationRecipient, Notification> notification = root.join("notification");

            predicates.add(cb.equal(root.get("recipientUser").get("userId"), userId));
            if (read != null) {
                predicates.add(cb.equal(root.get("read"), read));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("recipientStatus"), status));
            }
            if (type != null) {
                predicates.add(cb.equal(notification.get("notificationType"), type));
            }
            if (fromCreatedAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromCreatedAt));
            }
            if (toCreatedAt != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toCreatedAt));
            }
            if (fromReadAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("readAt"), fromReadAt));
            }
            if (toReadAt != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("readAt"), toReadAt));
            }
            if (search != null) {
                String pattern = "%" + search + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(notification.get("title")), pattern),
                        cb.like(cb.lower(notification.get("body")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
