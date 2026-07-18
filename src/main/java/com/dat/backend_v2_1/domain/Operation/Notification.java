package com.dat.backend_v2_1.domain.Operation;

import com.dat.backend_v2_1.enums.Operation.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "notification",
        schema = "operation",
        indexes = {
                @Index(name = "idx_notification_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_notification_type_created", columnList = "notification_type, created_at DESC")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "notification_id", nullable = false, updatable = false)
    UUID notificationId;

    @NotBlank
    @Size(max = 150)
    @Column(name = "title", nullable = false, length = 150)
    String title;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "body", nullable = false, length = 1000)
    String body;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "notification_type", nullable = false, length = 40)
    NotificationType notificationType = NotificationType.SYSTEM;

    @Size(max = 100)
    @Column(name = "reference_type", length = 100)
    String referenceType;

    @Size(max = 100)
    @Column(name = "reference_id", length = 100)
    String referenceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    String payload;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<NotificationRecipient> recipients = new ArrayList<>();
}
