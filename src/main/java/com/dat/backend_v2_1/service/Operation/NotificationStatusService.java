package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.enums.Operation.NotificationRecipientStatus;
import com.dat.backend_v2_1.repository.Operation.NotificationRecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationStatusService {

    private final NotificationRecipientRepository notificationRecipientRepository;
    private final ZoneId defaultZoneId;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID notificationId) {
        notificationRecipientRepository.updateStatusByNotificationId(
                notificationId,
                NotificationRecipientStatus.SENT,
                LocalDateTime.now(defaultZoneId)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID notificationId) {
        notificationRecipientRepository.updateStatusByNotificationId(
                notificationId,
                NotificationRecipientStatus.FAILED,
                null
        );
    }
}
