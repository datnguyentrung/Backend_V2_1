package com.dat.ai_receptionist_web.service.Notification;

import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Notification.NotificationRecipientMapper;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.enums.Operation.NotificationRecipientStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationRecipientService {
    private final NotificationRecipientRepository repository;
    private final NotificationRecipientMapper mapper;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationRecipientDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationRecipientDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public NotificationRecipientDTO.Response create(NotificationRecipientDTO.CreateRequest request) {
        NotificationRecipient entity = new NotificationRecipient();
        entity.setNotification(notificationRepository.findById(request.notificationId()).orElseThrow(() -> new IllegalArgumentException("Notification not found")));
        entity.setRecipientUser(userRepository.findById(request.recipientUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setRead(request.read());
        entity.setReadAt(request.readAt());
        entity.setDeliveredAt(request.deliveredAt());
        entity.setNotificationRecipientStatus(request.notificationRecipientStatus());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public NotificationRecipientDTO.Response update(UUID id, NotificationRecipientDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setNotification(notificationRepository.findById(request.notificationId()).orElseThrow(() -> new IllegalArgumentException("Notification not found")));
        entity.setRecipientUser(userRepository.findById(request.recipientUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setNotificationRecipientStatus(NotificationRecipientStatus.ARCHIVED);
    }

    private NotificationRecipient find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("NotificationRecipient not found"));
    }
}
