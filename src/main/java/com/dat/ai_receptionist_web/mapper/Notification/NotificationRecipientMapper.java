package com.dat.ai_receptionist_web.mapper.Notification;

import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import org.springframework.stereotype.Component;

@Component
public class NotificationRecipientMapper {
    public NotificationRecipientDTO.Response toResponse(NotificationRecipient entity) {
        if (entity == null) return null;
        return new NotificationRecipientDTO.Response(entity.getNotificationRecipientId(), entity.getNotification() == null ? null : entity.getNotification().getNotificationId(), entity.getRecipientUser() == null ? null : entity.getRecipientUser().getUserId(), entity.isRead(), entity.getReadAt(), entity.getDeliveredAt(), entity.getNotificationRecipientStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void updateEntity(NotificationRecipientDTO.UpdateRequest request, NotificationRecipient entity) {
        entity.setRead(request.read());
        entity.setReadAt(request.readAt());
        entity.setDeliveredAt(request.deliveredAt());
        entity.setNotificationRecipientStatus(request.notificationRecipientStatus());
    }
}
