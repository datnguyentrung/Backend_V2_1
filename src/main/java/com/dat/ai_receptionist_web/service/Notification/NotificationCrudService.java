package com.dat.ai_receptionist_web.service.Notification;

import com.dat.ai_receptionist_web.domain.Notification.Notification;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Notification.NotificationMapper;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationCrudService {
    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public NotificationDTO.Response create(NotificationDTO.CreateRequest request) {
        Notification entity = new Notification();
        entity.setTitle(request.title());
        entity.setBody(request.body());
        entity.setNotificationType(request.notificationType());
        entity.setReferenceType(request.referenceType());
        entity.setReferenceId(request.referenceId());
        entity.setPayload(request.payload());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public NotificationDTO.Response update(UUID id, NotificationDTO.UpdateRequest request) {
        var entity = find(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        repository.delete(entity);
    }

    private Notification find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Notification not found"));
    }
}
