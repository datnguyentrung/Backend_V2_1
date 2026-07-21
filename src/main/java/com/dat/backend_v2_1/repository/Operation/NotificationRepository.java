package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.Notification;
import com.dat.backend_v2_1.enums.Operation.NotificationType;
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
