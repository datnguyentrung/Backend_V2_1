package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.domain.Security.User;
import com.dat.backend_v2_1.enums.Operation.NotificationRecipientStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "notification_recipient",
        schema = "operation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_recipient",
                        columnNames = {"notification_id", "recipient_user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_nr_user_created", columnList = "recipient_user_id, created_at DESC"),
                @Index(name = "idx_nr_user_read_created", columnList = "recipient_user_id, is_read, created_at DESC"),
                @Index(name = "idx_nr_user_status_created", columnList = "recipient_user_id, recipient_status, created_at DESC"),
                @Index(name = "idx_nr_notification", columnList = "notification_id")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationRecipient {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "notification_recipient_id", nullable = false, updatable = false)
    UUID notificationRecipientId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "notification_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_recipient_notification")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    Notification notification;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "recipient_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_recipient_user")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    User recipientUser;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    boolean read = false;

    @Column(name = "read_at")
    LocalDateTime readAt;

    @Column(name = "delivered_at")
    LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "recipient_status", nullable = false, length = 30)
    NotificationRecipientStatus recipientStatus = NotificationRecipientStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
