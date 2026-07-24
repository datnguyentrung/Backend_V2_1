package com.dat.ai_receptionist_web.repository.Operation;

import com.dat.ai_receptionist_web.domain.Operation.Notification;
import com.dat.ai_receptionist_web.enums.Operation.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    boolean existsByNotificationTypeAndReferenceTypeAndReferenceId(
            NotificationType notificationType,
            String referenceType,
            String referenceId
    );
}
