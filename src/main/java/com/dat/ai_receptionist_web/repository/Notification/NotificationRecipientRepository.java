package com.dat.ai_receptionist_web.repository.Notification;
import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {
    @EntityGraph(attributePaths = {"notification", "recipientUser"})
    @Query("select nr from NotificationRecipient nr where nr.notification.notificationId = :id")
    List<NotificationRecipient> findDeliveryRows(@Param("id") UUID notificationId);
}
