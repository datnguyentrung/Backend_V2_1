package com.dat.backend_v2_1.dto.Operation;

import com.dat.backend_v2_1.dto.PageResponse;
import com.dat.backend_v2_1.enums.Operation.NotificationRecipientStatus;
import com.dat.backend_v2_1.enums.Operation.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class NotificationDTO {

    private NotificationDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CreateRequest {
        @NotBlank
        @Size(max = 150)
        String title;

        @NotBlank
        @Size(max = 1000)
        String body;

        NotificationType notificationType;

        @Size(max = 100)
        String referenceType;

        @Size(max = 100)
        String referenceId;

        String payload;

        @NotEmpty
        List<UUID> recipientUserIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecipientResponse {
        UUID notificationRecipientId;
        UUID notificationId;
        UUID recipientUserId;
        String title;
        String body;
        NotificationType notificationType;
        String referenceType;
        String referenceId;
        String payload;
        boolean read;
        LocalDateTime readAt;
        LocalDateTime deliveredAt;
        NotificationRecipientStatus recipientStatus;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class NotificationResponse {
        UUID notificationId;
        String title;
        String body;
        NotificationType notificationType;
        String referenceType;
        String referenceId;
        String payload;
        LocalDateTime createdAt;
        int recipientCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecipientListResponse {
        long unreadCount;
        PageResponse<RecipientResponse> notifications;
    }
}
