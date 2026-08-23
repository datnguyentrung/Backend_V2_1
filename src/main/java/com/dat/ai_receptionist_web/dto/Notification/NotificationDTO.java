package com.dat.ai_receptionist_web.dto.Notification;
import com.dat.ai_receptionist_web.enums.Operation.NotificationType;
import jakarta.validation.constraints.*;
import java.util.*;
public final class NotificationDTO {
    private NotificationDTO() {}
    public record CreateRequest(@NotBlank @Size(max=150) String title,
            @NotBlank @Size(max=1000) String body, @NotNull NotificationType type,
            @Size(max=100) String referenceType, @Size(max=100) String referenceId,
            String payload, Set<UUID> recipientUserIds, Set<UUID> recipientPersonIds,
            Set<@NotBlank String> recipientRoleCodes) {}
    public record Response(UUID notificationId, int recipientCount) {}
}
