package com.dat.ai_receptionist_web.dto.Notification;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Training.NotificationRecipientStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public final class NotificationRecipientDTO {
    private NotificationRecipientDTO() {
    }

    public record CreateRequest(@NotNull UUID notificationId, @NotNull UUID recipientUserId, boolean read, @NotNull LocalDateTime readAt, @NotNull LocalDateTime deliveredAt, @NotNull NotificationRecipientStatus notificationRecipientStatus) {
    }

    public record UpdateRequest(@NotNull UUID notificationId, @NotNull UUID recipientUserId, boolean read, @NotNull LocalDateTime readAt, @NotNull LocalDateTime deliveredAt, @NotNull NotificationRecipientStatus notificationRecipientStatus) {
    }

    public record Response(UUID notificationRecipientId, UUID notificationId, UUID recipientUserId, boolean read, LocalDateTime readAt, LocalDateTime deliveredAt, NotificationRecipientStatus notificationRecipientStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
