package com.dat.backend_v2_1.repository.Operation;

import com.dat.backend_v2_1.domain.Operation.NotificationRecipient;
import com.dat.backend_v2_1.enums.Operation.NotificationRecipientStatus;
import com.dat.backend_v2_1.enums.Operation.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {

    @EntityGraph(attributePaths = "notification")
    @Query(value = """
            SELECT nr
            FROM NotificationRecipient nr
            WHERE nr.recipientUser.userId = :userId
              AND (:read IS NULL OR nr.read = :read)
              AND (:status IS NULL OR nr.recipientStatus = :status)
              AND (:type IS NULL OR nr.notification.notificationType = :type)
              AND (:fromCreatedAt IS NULL OR nr.createdAt >= :fromCreatedAt)
              AND (:toCreatedAt IS NULL OR nr.createdAt <= :toCreatedAt)
              AND (:fromReadAt IS NULL OR nr.readAt >= :fromReadAt)
              AND (:toReadAt IS NULL OR nr.readAt <= :toReadAt)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(nr.notification.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(nr.notification.body) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
                    SELECT COUNT(nr)
                    FROM NotificationRecipient nr
                    WHERE nr.recipientUser.userId = :userId
                      AND (:read IS NULL OR nr.read = :read)
                      AND (:status IS NULL OR nr.recipientStatus = :status)
                      AND (:type IS NULL OR nr.notification.notificationType = :type)
                      AND (:fromCreatedAt IS NULL OR nr.createdAt >= :fromCreatedAt)
                      AND (:toCreatedAt IS NULL OR nr.createdAt <= :toCreatedAt)
                      AND (:fromReadAt IS NULL OR nr.readAt >= :fromReadAt)
                      AND (:toReadAt IS NULL OR nr.readAt <= :toReadAt)
                      AND (:search IS NULL OR :search = ''
                           OR LOWER(nr.notification.title) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(nr.notification.body) LIKE LOWER(CONCAT('%', :search, '%')))
                    """)
    Page<NotificationRecipient> filterForUser(
            @Param("userId") UUID userId,
            @Param("read") Boolean read,
            @Param("status") NotificationRecipientStatus status,
            @Param("type") NotificationType type,
            @Param("fromCreatedAt") LocalDateTime fromCreatedAt,
            @Param("toCreatedAt") LocalDateTime toCreatedAt,
            @Param("fromReadAt") LocalDateTime fromReadAt,
            @Param("toReadAt") LocalDateTime toReadAt,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "notification")
    Optional<NotificationRecipient> findByNotificationRecipientIdAndRecipientUser_UserId(
            UUID notificationRecipientId,
            UUID userId
    );

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
