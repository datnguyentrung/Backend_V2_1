package com.dat.ai_receptionist_web.service.Notification;
import com.dat.ai_receptionist_web.domain.Notification.*;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.enums.Operation.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.repository.Notification.*;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import lombok.RequiredArgsConstructor;
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
}
