package com.dat.ai_receptionist_web.service.Notification;
import com.dat.ai_receptionist_web.domain.Notification.*;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Operation.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.mapper.Notification.NotificationMapper;
import com.dat.ai_receptionist_web.repository.Notification.*;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.util.*;
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final UserPersonRepository userPersonRepository;
    private final NotificationDeliveryService deliveryService;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO.Response> list(Pageable pageable) {
        return PageResponse.of(notificationRepository.findAll(pageable), notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationDTO.Response get(UUID id) {
        return notificationMapper.toResponse(find(id));
    }

    @Transactional
    public NotificationDTO.Response create(NotificationDTO.CreateRequest request) {
        Set<UUID> recipientIds = resolveRecipients(request);
        if (recipientIds.isEmpty()) {
            throw new IllegalArgumentException("At least one notification recipient is required");
        }
        List<User> users = userRepository.findAllById(recipientIds);
        if (users.size() != recipientIds.size())
            throw new IllegalArgumentException("One or more notification recipients do not exist");
        Notification notification = notificationRepository.save(Notification.builder()
                .title(request.title()).body(request.body()).notificationType(request.type())
                .referenceType(request.referenceType()).referenceId(request.referenceId())
                .payload(request.payload()).build());
        users.forEach(user -> recipientRepository.save(NotificationRecipient.builder()
                .notification(notification).recipientUser(user).read(false)
                .notificationRecipientStatus(NotificationRecipientStatus.PENDING).build()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                deliveryService.deliver(notification.getNotificationId());
            }
        });
        return new NotificationDTO.Response(notification.getNotificationId(), users.size());
    }

    @Transactional
    public NotificationDTO.Response update(UUID id, NotificationDTO.UpdateRequest request) {
        Notification notification = find(id);
        notificationMapper.updateEntity(request, notification);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void delete(UUID id) {
        notificationRepository.delete(find(id));
    }

    private Set<UUID> resolveRecipients(NotificationDTO.CreateRequest request) {
        Set<UUID> recipients = new HashSet<>(safe(request.recipientUserIds()));
        safe(request.recipientPersonIds()).forEach(personId ->
                recipients.addAll(userPersonRepository.findActiveUserIdsByPersonId(personId)));
        safe(request.recipientRoleCodes()).stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .forEach(roleCode -> recipients.addAll(userRepository.findUserIdsByRoleCode(roleCode)));
        return recipients;
    }

    private <T> Set<T> safe(Set<T> values) {
        return values == null ? Set.of() : values;
    }

    private Notification find(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
    }
}
