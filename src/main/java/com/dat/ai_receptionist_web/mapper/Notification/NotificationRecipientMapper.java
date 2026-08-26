package com.dat.ai_receptionist_web.mapper.Notification;

import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotificationRecipientMapper {
    @Mapping(target = "notificationId", source = "notification.notificationId")
    @Mapping(target = "recipientUserId", source = "recipientUser.userId")
    NotificationRecipientDTO.Response toResponse(NotificationRecipient entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "notification", ignore = true)
    @Mapping(target = "recipientUser", ignore = true)
    @Mapping(target = "read", source = "read")
    @Mapping(target = "readAt", source = "readAt")
    @Mapping(target = "deliveredAt", source = "deliveredAt")
    @Mapping(target = "notificationRecipientStatus", source = "notificationRecipientStatus")
    void updateEntity(NotificationRecipientDTO.UpdateRequest request, @MappingTarget NotificationRecipient entity);
}
