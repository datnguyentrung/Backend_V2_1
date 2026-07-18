package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.NotificationRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID>, JpaSpecificationExecutor<NotificationRecipient> {

    @EntityGraph(attributePaths = {"notification", "recipientUser"})
    Page<NotificationRecipient> findAll(org.springframework.data.jpa.domain.Specification<NotificationRecipient> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"notification", "recipientUser"})
    Optional<NotificationRecipient> findByNotificationRecipientIdAndRecipientUser_UserId(
            UUID notificationRecipientId,
            UUID userId
    );

    boolean existsByNotificationRecipientIdAndRecipientUser_UserId(UUID notificationRecipientId, UUID userId);

    long countByRecipientUser_UserIdAndReadFalse(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationRecipient nr
            SET nr.read = true,
                nr.readAt = :readAt
            WHERE nr.notificationRecipientId = :notificationRecipientId
              AND nr.recipientUser.userId = :userId
              AND nr.read = false
            """)
    int markRead(
            @Param("notificationRecipientId") UUID notificationRecipientId,
            @Param("userId") UUID userId,
            @Param("readAt") LocalDateTime readAt
    );
}
