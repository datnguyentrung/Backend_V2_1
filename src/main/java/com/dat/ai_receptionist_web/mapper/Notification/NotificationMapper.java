package com.dat.ai_receptionist_web.mapper.Notification;

import com.dat.ai_receptionist_web.domain.Notification.Notification;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public NotificationDTO.Response toResponse(Notification entity) {
        if (entity == null) return null;
        return new NotificationDTO.Response(entity.getNotificationId(), entity.getTitle(), entity.getBody(), entity.getNotificationType(), entity.getReferenceType(), entity.getReferenceId(), entity.getPayload(), entity.getCreatedAt());
    }

    public void updateEntity(NotificationDTO.UpdateRequest request, Notification entity) {
        entity.setTitle(request.title());
        entity.setBody(request.body());
        entity.setNotificationType(request.notificationType());
        entity.setReferenceType(request.referenceType());
        entity.setReferenceId(request.referenceId());
        entity.setPayload(request.payload());
    }
}
